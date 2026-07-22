# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Nota: la documentación de este repositorio y los comentarios del código están en
> **español**. Redacta comentarios, mensajes y documentación en español.

## Qué es esto

Aplicación web renderizada en servidor que permite a usuarios del IMSS autenticarse contra
IBM Content Manager OnDemand (CMOD) en z/OS, navegar los folders que tienen asignados y
ejecutar búsquedas — todo a través de la API Java **ODWEK** de IBM (`com.ibm.edms.od.*`).
Spring Boot 2.x (ojo: espacio de nombres `javax.*`, no `jakarta.*`), vistas FreeMarker
(`.ftlh`) + Tailwind (CDN), con **WebSphere Liberty** como runtime destino.

## Gotcha crítico de estructura del repo

Hay **dos copias de cada archivo fuente**:

- **`src/main/...`** — el árbol canónico y vigente. Raíz de paquete `com.app.icncards`.
- **Raíz del repo (`./*.java`, `./*.ftlh`, `./*.js`, `application.yml`, ...)** — una
  **copia plana obsoleta** con la raíz de paquete antigua `mx.infotec.imss`. Es previa al
  rename a `com.app.icncards` (commit a7bf139) y no forma parte del layout de build.

**Siempre edita los archivos bajo `src/main/`.** Ignora los duplicados en la raíz; no
propagues cambios hacia ellos. El documento `arquitectura-pool-odwek.md` también sigue
referenciando el paquete viejo `mx.infotec.imss` — considera sus nombres de paquete
desactualizados, pero sus diagramas y descripción de comportamiento siguen siendo válidos.

## Build y ejecución

No hay `pom.xml` / `build.gradle` versionado, y no existe una clase `@SpringBootApplication`
en el árbol — la app se compila y despliega como WAR dentro de un servidor Liberty externo
(`server.xml`) usando los jars de ODWEK (no están en Maven público). Tal cual está el repo
no hay comandos de build/test/lint ejecutables; se requiere un entorno de build externo con
la dependencia ODWEK y un empaquetado WAR de Spring Boot. No inventes comandos de build.

La configuración de runtime vive en `src/main/resources/application.yml` y se alimenta de
variables de entorno (valores reales en `server.env`, no versionado):
`APP_SECURITY_CIPHER_KEY` (llave AES), `ONDEMAND_HOST`, `ONDEMAND_KEYRING`,
`ONDEMAND_KEYSTASH`.

## Arquitectura (hexagonal / puertos y adaptadores)

```
com.app.icncards
├── domain/model         Tipos de dominio puros (FolderSummary, FolderSearchDefinition,
│                        SearchFieldDefinition/Kind, SearchOperator, UserLoginInfo).
│                        Sin framework, sin ODWEK.
├── application
│   ├── port/out         FolderRepository — puerto de salida del que depende el dominio.
│   └── service          FolderService — casos de uso; hogar natural de reglas futuras
│                        (orden, filtros, cache).
└── infrastructure
    ├── odwek            Todo lo que conoce la API de ODWEK vive aquí (y solo aquí).
    ├── security         Spring Security + sellado de credenciales.
    └── web              Controllers, forms, @ControllerAdvice.
```

Regla de dependencia: `domain` y `application` nunca importan `com.ibm.edms.od.*` ni Spring
web. Solo `infrastructure/odwek` toca tipos de ODWEK (`ODServer`, `ODFolder`, `ODCriteria`,
`Enumeration`). `CmodFolderRepository` es el adaptador de salida que implementa
`FolderRepository`.

## Capa de conexión ODWEK (el corazón de la app)

`infrastructure/odwek` envuelve ODWEK como `JdbcTemplate` envuelve JDBC. Lee
`arquitectura-pool-odwek.md` para los diagramas completos. Piezas clave:

- **`ODServerPool` / `ODServerPooledObjectFactory`** (`pool/`) — pool de Apache Commons
  Pool2 de **"shells" `ODServer`**: instancias `initialize()`das pero **sin** logon. El
  logon/logoff es por transacción y por usuario — nunca se agrupa en el pool. El warm-up al
  arranque y el shutdown son ciclo de vida `@PostConstruct`/`@PreDestroy`. `pool.max-total`
  ≈ máximo de transacciones CMOD concurrentes; cruzarlo contra el límite de conexiones /
  licencia del servidor z/OS.
- **`OnDemandTemplate implements OnDemandOperations`** (`connection/`) — el único método
  que llama el código de negocio: `execute(credentials, callback)`. Corre
  `borrow → logon → callback.doInServer(server) → logoff (finally) → release|invalidate`.
  El código de negocio/adaptador pasa un lambda `ODServerCallback` y nunca toca el pool ni
  `ODServer` directamente.
- **release vs invalidate** — la decisión de correctitud central. Un logon rechazado por
  RACF (credenciales inválidas / password vencido) deja el shell sano → **`release`** de
  vuelta al pool. Una falla *técnica* lo deja sospechoso → **`invalidate`** (destruir +
  recrear). Por esto nunca revocamos un userid por reintentos ni reusamos una conexión rota.
- **`ODErrorClassifier`** — mapea los `errorId` de ODWEK a auth / password-vencido /
  técnico, usando las listas de ids en `application.yml`
  (`ondemand.auth-error-ids: [2107]`, `ondemand.password-expired-error-ids: [2061]`). Si
  `auth-error-ids` está vacío, los passwords incorrectos se clasifican mal como error
  técnico — mantén esas listas exactas.
- Los passwords en texto plano se ponen en cero (`Arrays.fill(pwd, '\0')`) en los bloques
  `finally` de toda la capa; conserva esta higiene al editar rutas de credenciales.

## Autenticación y flujo de sesión

La autenticación se hace **en el controller**, no por el filtro `formLogin` de Spring, para
poder validar campos vacíos en backend, aplicar el bloqueo de intentos *antes* de llegar a
RACF y mostrar mensajes por campo. `SecurityConfig` cablea un `ProviderManager` con nuestro
provider para uso programático.

- **`OnDemandAuthenticationProvider`** autentica realizando un **logon real de ODWEK** (vía
  `OnDemandOperations.execute`). Al tener éxito: (a) sella la credencial con AES-GCM
  (`AesGcmCredentialCipher` → `SessionCredential`) y (b) lee de forma oportunista info de
  `ODUser` (último logon, logins fallidos, días para expirar el password) en el *mismo*
  logon — todo best-effort, nunca se le permite tumbar el login. Los resultados mapean a
  excepciones de Spring: `BadCredentialsException` (rechazo RACF),
  `CredentialsExpiredException` (vencido — **nunca** cuenta como intento fallido),
  `AuthenticationServiceException` (técnico).
- **`LoginAttemptService`** bloquea por `app.security.login.max-attempts` (2 — RACF revoca
  al 3, así que cortamos antes) con un enfriamiento `lock-minutes` que *no* reinicia el
  contador de RACF.
- **Reuso de credencial sellada**: tras el login no se guarda el password en texto plano.
  `CurrentUserCredentials` / `SessionBackedCredentials` desellan el `SessionCredential`
  guardado bajo demanda para que `CmodFolderRepository` pueda re-hacer logon en cada
  operación de folder/búsqueda.
- **Cambio de password vencido**: `ChangePasswordController` es una ruta **pública** (el
  usuario aún no puede iniciar sesión) que usa el `logon(host, user, oldPwd, newPwd)` de 4
  argumentos de ODWEK. El password viejo se vuelve a pedir en ese formulario en lugar de
  arrastrarlo del login fallido, para no retener texto plano entre peticiones.
- **Propiedad del timeout de sesión**: el timeout real de invalidación de sesión y la
  configuración de cookies viven en el `server.xml` de Liberty, **no** en este código.
  `app.security.idle.timeout-minutes` solo alimenta `idle-timeout.js`, un logout proactivo
  del lado del cliente (UX, mantenerlo ≤ el timeout del servidor). El logout por inactividad
  hace POST a `/logout?reason=idle` para que `logoutSuccessHandler` muestre un mensaje
  distinto.
- CSRF activo por defecto; la CSP en `SecurityConfig` permite `cdn.tailwindcss.com`.
  `expose-request-attributes: true` en `application.yml` es requerido para que las
  plantillas `.ftlh` lean el token `_csrf`.

## Convenciones de la capa web

Controllers en `infrastructure/web/controller`, forms en `.../form`, manejo transversal de
errores en `.../advice/GlobalExceptionHandler` y datos de modelo compartidos en
`GlobalModelAttributes`. Las vistas son FreeMarker `.ftlh` bajo
`src/main/resources/templates`; JS/CSS estático bajo `static/`. Las cadenas visibles al
usuario viven en `messages.properties` (español) — agrega claves de mensaje ahí en vez de
texto hardcodeado en plantillas o controllers.

### Plantilla base FreeMarker (`_layout.ftlh`)

FreeMarker no tiene herencia tipo Tiles/Thymeleaf-layout; se emula con **macros + `<#nested>`**.
`templates/_layout.ftlh` centraliza lo que antes se duplicaba en cada vista y expone macros
reutilizables (se importan como `<#import "_layout.ftlh" as ui/>`):

- `<@ui.page title=... noindex=false bodyClass="" idle=false>…contenido…</@ui.page>` — shell
  completo: `<head>` (con CSS/Tailwind), `<body>`, y si `idle=true` agrega
  `data-idle-timeout-minutes` + el form/script de inactividad. El contenido propio de la vista
  va como `<#nested>`.
- `<@ui.appHeader/>` (marca + cerrar sesión, pantallas internas), `<@ui.recordStrip rightLabel rightValue/>`
  (tira negra de las tarjetas), `<@ui.brand cls=""/>`, `<@ui.footer/>`, `<@ui.csrf/>`.

Al crear/editar una vista: úsala sobre `<@ui.page>`, **no** repitas `<!DOCTYPE>`/`<head>`/header/footer
(deben existir solo en `_layout.ftlh`). El data model (`_csrf`, `idleTimeoutMinutes`, mensajes)
es **global** en todos los namespaces, por lo que las macros lo leen sin recibirlo por parámetro.
El título se pasa ya resuelto: para mensajes, captúralo antes con
`<#assign pageTitle><@spring.message "x.title"/></#assign>`.

### Rutas: siempre `<@spring.url>`

Toda ruta de recurso o endpoint en los `.ftlh` (CSS, JS, `href`, `action`) se genera con
**`<@spring.url '/ruta'/>`** — nunca rutas absolutas hardcodeadas — para respetar el context
root del WAR en Liberty. Funciona igual en cualquier atributo (`href`, `action`, `src`); usa
comillas simples dentro de la macro. **No** uses `${request.contextPath}`: el objeto `request`
no está expuesto en el modelo (`expose-request-attributes` expone *atributos*, no el `request`),
mientras que `<@spring.url>` se apoya en `springMacroRequestContext` (el mismo helper de
`<@spring.message>`, siempre disponible). Ver [[ftlh-rutas-spring-url]] en memoria.

Casos borde: en `theme.css` (estático, la macro no aplica) las fuentes usan **ruta relativa**
(`url("../fonts/…")`), que resuelve respecto al CSS y también es context-safe. Y si un `.js`
necesita ubicar un form, que lo haga por `id`/`btn.form`, **no** por `form[action="/ruta"]`
(quedaría acoplado a la ruta literal que ahora genera la macro).

`infrastructure/**/devtest/` (p. ej. `OdwekConnectionTestRunner`,
`SessionCredentialDebugController`) son ayudantes solo de diagnóstico — no los cables en
flujos de producción.

## Referencias oficiales de IBM ODWEK

Fuentes autoritativas para decidir mejores prácticas y comportamiento de la API. Consúltalas
(vía WebFetch) antes de asumir comportamiento de ODWEK; la app está en la versión **10.5**.

- **ODWEK Basics and Beyond** (Redbook, guía de mejores prácticas):
  https://cmod.wiki/dox/ODWEK-BasicsAndBeyond.pdf — capítulos relevantes:
  - Cap. 5 (p.107) Introducción a mejores prácticas, hints y tips
  - Cap. 6 (p.111) **Connection pooling y manejo de conexiones** (base del diseño de
    `ODServerPool` / `OnDemandTemplate`)
  - Cap. 7 (p.143) Globalización (folders por idioma → llamar `getFolders()` antes de
    `openFolder()`; ver nota en `CmodFolderRepository`)
  - Cap. 8 (p.161) Búsqueda de folders
  - Cap. 9 (p.179) Recuperación de documentos
  - Cap. 10 (p.197) Applets, plug-ins y transforms
  - Cap. 11 (p.215) Almacenamiento y actualización de documentos
  - Cap. 12 (p.223) Memoria y rendimiento
  - Cap. 13 (p.251) Troubleshooting
- **Guía de programación ODWEK v10.5** (PDF oficial IBM):
  https://publibfp.dhe.ibm.com/epubs/pdf/c1933533.pdf
- **Ejemplos por escenario (docs web IBM, CMOD 10.5)**:
  https://www.ibm.com/docs/bg/cmofm/10.5.0?topic=kit-developing-java-applications
- **Javadoc de la librería ODWEK v10.5**:
  https://cmod.wiki/dox/ODapiDoc/v10.5/index.html
