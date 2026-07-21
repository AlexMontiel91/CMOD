# Módulo OnDemand — Pool de conexiones ODServer

Este documento describe **la capa de conexión a OnDemand**: el pool de objetos
`ODServer` + el template que orquesta cada transacción. Es el corazón del sistema
y el resto de la app se apoya en él.

Nota de alcance: la app ya creció más allá de esta capa. Hoy existen también la
autenticación (`infrastructure/security/OnDemandAuthenticationProvider`, que
produce las credenciales), la búsqueda de folders
(`infrastructure/odwek/adapter/CmodFolderRepository`, que consume
`OnDemandOperations`) y el cambio de password vencido. Todos esos consumidores
entran por el mismo `OnDemandOperations` que aquí se documenta; los diagramas de
abajo cubren la capa de conexión, no esos casos de uso.

Paquete raíz: `com.app.icncards.infrastructure.odwek`

```
infrastructure/odwek/
├── config/        OnDemandConfiguration, OnDemandProperties
├── connection/    OnDemandOperations, OnDemandTemplate, ODServerCallback,
│                  OnDemandCredentials, ODErrorClassifier, excepciones
└── pool/          ODServerPool, ODServerPooledObjectFactory
```

---

## 1. Diagrama de clases

Relaciones entre las piezas del módulo. `OnDemandConfiguration` crea los beans;
`OnDemandTemplate` (adaptador del puerto `OnDemandOperations`) orquesta el pool,
el clasificador de errores y las credenciales.

```mermaid
classDiagram
    direction LR

    class OnDemandConfiguration {
        <<Configuration>>
        +odConfig() ODConfig
        +odErrorClassifier() ODErrorClassifier
        +odServerPool() ODServerPool
        +onDemandTemplate() OnDemandOperations
    }

    class OnDemandProperties {
        <<ConfigurationProperties>>
        +String serverName
        +int port
        +int maxHits
        +Pool pool
        +Ssl ssl
    }

    class OnDemandOperations {
        <<interface>>
        +execute(creds, action) T
        +changeExpiredPassword(user, oldPwd, newPwd) void
    }

    class OnDemandTemplate {
        -ODServerPool pool
        -OnDemandProperties props
        -ODErrorClassifier classifier
        +execute(creds, action) T
        +changeExpiredPassword(user, oldPwd, newPwd) void
    }

    class ODServerCallback {
        <<interface>>
        +doInServer(server) T
    }

    class OnDemandCredentials {
        <<interface>>
        +getUser() String
        +password() chars
    }

    class ODErrorClassifier {
        +isAuthFailure(e) boolean
        +isPasswordExpired(e) boolean
        +translateLogon(e) OnDemandException
        +translate(e) OnDemandException
    }

    class ODServerPool {
        -GenericObjectPool pool
        +start() void
        +shutdown() void
        +borrow() ODServer
        +release(server) void
        +invalidate(server) void
    }

    class ODServerPooledObjectFactory {
        +create() ODServer
        +validateObject(p) boolean
        +destroyObject(p) void
    }

    OnDemandOperations <|.. OnDemandTemplate : implementa
    OnDemandConfiguration ..> ODServerPool : crea
    OnDemandConfiguration ..> OnDemandTemplate : crea
    OnDemandConfiguration ..> ODErrorClassifier : crea
    OnDemandConfiguration ..> OnDemandProperties : usa
    OnDemandTemplate --> ODServerPool : usa
    OnDemandTemplate --> ODErrorClassifier : usa
    OnDemandTemplate ..> OnDemandCredentials : recibe
    OnDemandTemplate ..> ODServerCallback : ejecuta
    ODServerPool *-- ODServerPooledObjectFactory : contiene
    ODServerPooledObjectFactory ..> OnDemandProperties : usa
```

---

## 2. Diagrama de secuencia — una transacción

Camino de `execute(...)`: tomar un shell del pool, logon con la credencial del
usuario, ejecutar la acción de negocio y, en el `finally`, logoff con devolución
o invalidación según el tipo de error.

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente (servicio)
    participant T as OnDemandTemplate
    participant P as ODServerPool
    participant S as ODServer
    participant M as OnDemand (mainframe)

    C->>T: execute(cred, accion)
    T->>P: borrow()
    P->>P: validateObject (isInitialized)
    P-->>T: ODServer (shell sin logon)
    T->>S: logon(host, user, pwd)
    S->>M: autenticación RACF

    alt logon rechazado (auth)
        M-->>S: error de credenciales
        T->>P: release(server)
        Note over T,P: shell sano: vuelve al pool
        T-->>C: InvalidCredentialsException
    else error técnico (red / servidor)
        M-->>S: error técnico
        T->>P: invalidate(server)
        Note over T,P: shell sospechoso: se destruye y recrea
        T-->>C: OnDemandException
    else logon ok
        M-->>S: sesión establecida
        T->>S: accion.doInServer(server)
        Note over S,M: openFolder / search / retrieve<br/>(pendiente: incremento documentos)
        S-->>T: resultado
        T->>S: logoff() (en finally)
        T->>P: release(server)
        T-->>C: resultado
    end
```

---

## 2b. Diagrama de secuencia — cambio de password vencido

`changeExpiredPassword(...)` no ejecuta una acción de negocio: su único fin es el
**logon de 4 argumentos** de ODWEK, donde RACF exige la contraseña **vencida** para
autorizar el cambio a la nueva. No hay `logoff` de una sesión de trabajo (nunca se
abrió una sesión normal); el shell se devuelve o invalida según el tipo de error,
con la misma regla auth/técnico.

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente (ChangePasswordController)
    participant T as OnDemandTemplate
    participant P as ODServerPool
    participant S as ODServer
    participant M as OnDemand (mainframe)

    C->>T: changeExpiredPassword(user, oldPwd, newPwd)
    T->>P: borrow()
    P-->>T: ODServer (shell sin logon)
    T->>S: logon(host, user, oldPwd, newPwd)
    S->>M: cambio de password RACF

    alt password viejo inválido / aún vencido (auth)
        M-->>S: error de credenciales
        T->>P: release(server)
        Note over T,P: shell sano: vuelve al pool
        T-->>C: OnDemandException (traducida)
    else error técnico (red / servidor)
        M-->>S: error técnico
        T->>P: invalidate(server)
        Note over T,P: shell sospechoso: se destruye y recrea
        T-->>C: OnDemandException
    else cambio aceptado
        M-->>S: password actualizado
        T->>P: release(server)
        Note over T: en finally: se limpian oldPwd y newPwd (Arrays.fill)
        T-->>C: (void) OK
    end
```

---

## 3. Ciclo de vida del pool (independiente de la transacción)

```mermaid
flowchart TD
    A[Arranque de la app] --> B["@PostConstruct start()"]
    B --> C{"warm-up activado?"}
    C -- sí --> D["preparePool(): crea min-idle shells<br/>new ODServer + initialize (sin logon)"]
    C -- no --> E[conexiones se crean bajo demanda]
    D --> F[Pool listo: N idle]
    E --> F
    F -. atiende transacciones .-> F

    G[Baja de la app] --> H["@PreDestroy shutdown()"]
    H --> I["pool.close()"]
    I --> J["destroyObject por cada shell:<br/>logoff + terminate"]
```

---

## Notas

- El **pool guarda shells** (`ODServer` inicializados **sin** logon). El logon/logoff
  por usuario ocurre por transacción dentro del template.
- **Validación en borrow** vía `isInitialized()`. Si es false, Commons Pool2 destruye
  el shell y crea uno nuevo de forma transparente. La conexión TCP muerta no la
  detecta `isInitialized`; la atrapa el template al fallar el `logon`.
- **auth vs técnico**: un logon rechazado por RACF deja el shell sano (`release`);
  un error técnico lo marca sospechoso (`invalidate`). Así no se revoca el userid
  por reintentos ni se reutiliza una conexión rota.
- `max-total` = número de **transacciones OnDemand concurrentes** (cruzar con el
  límite de conexiones/licencia del servidor z/OS).
