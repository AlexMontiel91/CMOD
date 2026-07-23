<#import "/spring.ftl" as spring/>
<#import "_layout.ftlh" as ui/>
<#assign pageTitle><@spring.message "login.title"/></#assign>
<@ui.page title=pageTitle noindex=true bodyClass="min-h-screen flex items-center justify-center px-4 py-10">

    <div class="w-full max-w-md">
        <div class="surface overflow-hidden">
            <@ui.recordStrip rightLabelKey="login.recordStrip.label" rightValueKey="login.recordStrip.value"/>

            <div class="p-8 sm:p-10">
                <div class="mb-7">
                    <@ui.brand cls="mb-5"/>
                    <h1 class="h-display text-2xl"><@spring.message "login.title"/></h1>
                    <p class="meta mt-2">&gt; <@spring.message "login.subtitle"/></p>
                </div>

                <#-- Mensaje de logout (success) -->
                <#if logout?? && logout>
                    <div class="alert alert-success mb-5 px-4 py-3 text-sm">
                        <@spring.message "login.logout.success"/>
                    </div>
                </#if>

                <#-- Password cambiado exitosamente (success) -->
                <#if passwordChanged?? && passwordChanged>
                    <div class="alert alert-success mb-5 px-4 py-3 text-sm">
                        <@spring.message "login.password.changed"/>
                    </div>
                </#if>

                <#-- Sesion expirada / credencial ya no valida (warning) -->
                <#if expired?? && expired>
                    <div class="alert alert-warning mb-5 px-4 py-3 text-sm">
                        <@spring.message "login.session.expired"/>
                    </div>
                </#if>

                <#-- Logout por inactividad (warning) -->
                <#if idle?? && idle>
                    <div class="alert alert-warning mb-5 px-4 py-3 text-sm">
                        <@spring.message "login.session.idle"/>
                    </div>
                </#if>

                <#-- Acceso denegado / CSRF invalido (warning) -->
                <#if denied?? && denied>
                    <div class="alert alert-warning mb-5 px-4 py-3 text-sm">
                        <@spring.message "login.access.denied"/>
                    </div>
                </#if>

                <#-- Errores globales (credenciales, bloqueo, servicio) -->
                <@spring.bind "loginForm"/>
                <#if spring.status.errorMessages?size gt 0>
                    <div class="alert alert-error mb-5 px-4 py-3 text-sm">
                        <#list spring.status.errorMessages as msg>
                            <div>${msg}</div>
                        </#list>
                    </div>
                </#if>

                <form method="post" action="<@spring.url '/login'/>" autocomplete="off" novalidate class="space-y-5">
                    <@ui.csrf/>

                    <#-- Usuario -->
                    <div>
                        <label for="username" class="app-label"><@spring.message "login.username.label"/></label>
                        <@spring.bind "loginForm.username"/>
                        <input type="text" id="username" name="username"
                               value="${spring.status.value!''}"
                               autocomplete="off" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-app w-full px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <#-- Contrasena (NUNCA se re-renderiza el value) -->
                    <div>
                        <label for="password" class="app-label"><@spring.message "login.password.label"/></label>
                        <@spring.bind "loginForm.password"/>
                        <input type="password" id="password" name="password"
                               autocomplete="new-password" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-app w-full px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <button type="submit" id="login-submit-btn"
                            data-loading-text="<@spring.message "login.submitting"/>"
                            class="btn-primary w-full text-sm py-3 mt-2 flex items-center justify-center gap-2">
                        <svg id="login-spinner" class="hidden animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg"
                             fill="none" viewBox="0 0 24 24" aria-hidden="true">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                        </svg>
                        <span id="login-submit-text"><@spring.message "login.submit"/></span>
                    </button>
                </form>
            </div>
        </div>

        <@ui.footer/>
    </div>

    <script src="<@spring.url '/js/login-submit.js'/>"></script>
</@ui.page>
