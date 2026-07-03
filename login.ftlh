<#import "/spring.ftl" as spring/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title><@spring.message "login.title"/> — IMSS</title>
    <link rel="stylesheet" href="/css/theme.css">
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen flex items-center justify-center px-4">

    <div class="w-full max-w-md">
        <div class="bg-white rounded-2xl shadow-xl overflow-hidden">
            <div class="accent-bar h-1.5"></div>

            <div class="p-8 sm:p-10">
                <div class="mb-8">
                    <div class="w-11 h-11 rounded-xl flex items-center justify-center mb-4"
                         style="background-color: var(--imss-red);">
                        <span class="text-white font-bold text-lg">IM</span>
                    </div>
                    <h1 class="text-2xl font-bold tracking-tight" style="color: var(--imss-black);">
                        <@spring.message "login.title"/>
                    </h1>
                    <p class="text-sm text-secondary mt-1"><@spring.message "login.subtitle"/></p>
                </div>

                <#-- Mensaje de logout (success) -->
                <#if logout?? && logout>
                    <div class="alert-success mb-5 rounded-lg px-4 py-3 text-sm">
                        <@spring.message "login.logout.success"/>
                    </div>
                </#if>

                <#-- Sesion expirada / credencial ya no valida (warning) -->
                <#if expired?? && expired>
                    <div class="alert-warning mb-5 rounded-lg px-4 py-3 text-sm">
                        <@spring.message "login.session.expired"/>
                    </div>
                </#if>

                <#-- Logout por inactividad (warning) -->
                <#if idle?? && idle>
                    <div class="alert-warning mb-5 rounded-lg px-4 py-3 text-sm">
                        <@spring.message "login.session.idle"/>
                    </div>
                </#if>

                <#-- Acceso denegado / CSRF invalido (warning) -->
                <#if denied?? && denied>
                    <div class="alert-warning mb-5 rounded-lg px-4 py-3 text-sm">
                        <@spring.message "login.access.denied"/>
                    </div>
                </#if>

                <#-- Errores globales (credenciales, bloqueo, servicio) -->
                <@spring.bind "loginForm"/>
                <#if spring.status.errorMessages?size gt 0>
                    <div class="alert-error mb-5 rounded-lg px-4 py-3 text-sm">
                        <#list spring.status.errorMessages as msg>
                            <div>${msg}</div>
                        </#list>
                    </div>
                </#if>

                <form method="post" action="/login" autocomplete="off" novalidate class="space-y-5">
                    <#-- CSRF (requiere spring.freemarker.expose-request-attributes=true) -->
                    <#if _csrf??>
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    </#if>

                    <#-- Usuario -->
                    <div>
                        <label for="username" class="block text-sm font-medium mb-1.5" style="color: var(--imss-charcoal);">
                            <@spring.message "login.username.label"/>
                        </label>
                        <@spring.bind "loginForm.username"/>
                        <input type="text" id="username" name="username"
                               value="${spring.status.value!''}"
                               autocomplete="off" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-imss w-full rounded-lg border px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <#-- Contrasena (NUNCA se re-renderiza el value) -->
                    <div>
                        <label for="password" class="block text-sm font-medium mb-1.5" style="color: var(--imss-charcoal);">
                            <@spring.message "login.password.label"/>
                        </label>
                        <@spring.bind "loginForm.password"/>
                        <input type="password" id="password" name="password"
                               autocomplete="new-password" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-imss w-full rounded-lg border px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <button type="submit" class="btn-imss w-full rounded-lg text-sm font-medium py-3 mt-2">
                        <@spring.message "login.submit"/>
                    </button>
                </form>
            </div>
        </div>

        <p class="text-center text-xs text-secondary mt-6">Instituto Mexicano del Seguro Social</p>
    </div>

</body>
</html>
