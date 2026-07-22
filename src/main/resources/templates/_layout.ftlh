<#--
    Plantilla base / fragmentos reutilizables.

    FreeMarker no tiene herencia tipo Tiles/Thymeleaf-layout; se emula con macros
    + <#nested>. Aqui se centraliza lo que antes se duplicaba en cada vista: el
    <head>, el encabezado, la tira de registro, el pie, el CSRF y el form/script
    de inactividad.

    El data model (_csrf, idleTimeoutMinutes, mensajes) es GLOBAL en todos los
    namespaces, por lo que las macros lo leen sin recibirlo por parametro.

    Las rutas de recursos y endpoints se generan con <@spring.url ...> para que
    respeten el context root del WAR en Liberty (no asumir despliegue en "/").
-->
<#import "/spring.ftl" as spring/>

<#-- Campo oculto CSRF (requiere spring.freemarker.expose-request-attributes=true) -->
<#macro csrf><#if _csrf??><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/></#if></#macro>

<#-- Marca: logo SVG + nombre. cls: clases extra (p. ej. margen).
     El SVG es un placeholder (líneas apiladas ~ índice/archivo); reemplazar por el real. -->
<#macro brand cls="">
    <div class="brand<#if cls?has_content> ${cls}</#if>">
        <svg class="brand-logo" width="28" height="28" viewBox="0 0 28 28" role="img" aria-label="<@spring.message "app.name"/>">
            <rect width="28" height="28" rx="5" fill="#D81020"/>
            <rect x="7" y="8" width="14" height="2.6" rx="1.3" fill="#fff"/>
            <rect x="7" y="13" width="14" height="2.6" rx="1.3" fill="#fff"/>
            <rect x="7" y="18" width="9" height="2.6" rx="1.3" fill="#fff"/>
        </svg>
        <span><@spring.message "app.name"/></span>
    </div>
</#macro>

<#-- Encabezado de pantallas internas: marca a la izquierda; a la derecha bienvenida
     + cerrar sesion, y debajo la ultima conexion + intentos fallidos. Los datos de
     sesion (username, lastLogonFormatted/firstLogin, failedLogins) los expone
     GlobalModelAttributes, disponibles en toda vista autenticada. -->
<#macro appHeader>
    <header class="app-header">
        <div class="app-header__inner max-w-7xl mx-auto px-6 py-3">
            <@brand/>
            <div class="app-header__session">
                <div class="app-header__top">
                    <#if username?? && username?has_content>
                        <span class="app-header__welcome"><@spring.message "home.welcome"/>, ${username}</span>
                    </#if>
                    <form method="post" action="<@spring.url '/logout'/>">
                        <@csrf/>
                        <button type="submit" class="link app-header__logout"><@spring.message "home.logout"/></button>
                    </form>
                </div>
                <#if lastLogonFormatted?? || (firstLogin?? && firstLogin) || failedLogins??>
                    <div class="app-header__conn">
                        <#if lastLogonFormatted??>
                            <@spring.message "home.lastLogin.label"/> <span class="rv">${lastLogonFormatted}</span>
                        <#elseif firstLogin?? && firstLogin>
                            <@spring.message "home.firstLogin"/>
                        </#if>
                        <#if failedLogins??>
                            <#if lastLogonFormatted?? || (firstLogin?? && firstLogin)> · </#if>
                            <@spring.message "home.failedLogins.label"/> <span class="rv">${failedLogins}</span>
                        </#if>
                    </div>
                </#if>
            </div>
        </div>
    </header>
</#macro>

<#-- Tira de registro (tope de las tarjetas de acceso/estado). -->
<#macro recordStrip rightLabel rightValue>
    <div class="record-strip">
        <span>SIST <span class="rv">ICN·CMOD</span></span>
        <span>${rightLabel} <span class="rv">${rightValue}</span></span>
    </div>
</#macro>

<#-- Pie de pagina. -->
<#macro footer>
    <p class="meta text-center mt-6"><@spring.message "app.footer"/></p>
</#macro>

<#--
    Shell de pagina: <head> + <body> + (opcional) form/script de inactividad.
      title:     texto ya resuelto; se le agrega " — {app.name}".
      noindex:   agrega <meta robots noindex,nofollow>.
      bodyClass: clases del <body>.
      idle:      si true, agrega data-idle-timeout-minutes + form/script de inactividad.
-->
<#macro page title noindex=false bodyClass="" idle=false>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <#if noindex><meta name="robots" content="noindex, nofollow"></#if>
    <title>${title} — <@spring.message "app.name"/></title>
    <link rel="stylesheet" href="<@spring.url '/css/theme.css'/>">
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body<#if bodyClass?has_content> class="${bodyClass}"</#if><#if idle> data-idle-timeout-minutes="${idleTimeoutMinutes!0}"</#if>>
<#nested>
<#if idle>
    <#-- Form oculto para el logout por inactividad (idle-timeout.js). CSRF-protegido. -->
    <form id="idle-logout-form" method="post" action="<@spring.url '/logout?reason=idle'/>" class="hidden">
        <@csrf/>
    </form>
    <script src="<@spring.url '/js/idle-timeout.js'/>"></script>
</#if>
</body>
</html>
</#macro>
