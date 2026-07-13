<#import "/spring.ftl" as spring/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title><@spring.message "changePassword.title"/> — <@spring.message "app.name"/></title>
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
                         style="background-color: var(--app-red);">
                        <span class="text-white font-bold text-lg"><@spring.message "app.logoInitials"/></span>
                    </div>
                    <h1 class="text-2xl font-bold tracking-tight" style="color: var(--app-black);">
                        <@spring.message "changePassword.title"/>
                    </h1>
                </div>

                <#-- Explica POR QUE llego aqui (no pudo autenticar con normalidad) -->
                <div class="alert-warning mb-6 rounded-lg px-4 py-3 text-sm">
                    <@spring.message "changePassword.reason"/>
                </div>

                <@spring.bind "changePasswordForm"/>
                <#if spring.status.errorMessages?size gt 0>
                    <div class="alert-error mb-5 rounded-lg px-4 py-3 text-sm">
                        <#list spring.status.errorMessages as msg>
                            <div>${msg}</div>
                        </#list>
                    </div>
                </#if>

                <form method="post" action="/change-password" autocomplete="off" novalidate class="space-y-5">
                    <#if _csrf??>
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    </#if>

                    <@spring.bind "changePasswordForm.user"/>
                    <input type="hidden" name="user" value="${spring.status.value!''}"/>
                    <div>
                        <label class="block text-sm font-medium mb-1.5" style="color: var(--app-charcoal);">
                            <@spring.message "login.username.label"/>
                        </label>
                        <div class="input-app w-full rounded-lg border px-3.5 py-2.5 text-sm text-secondary"
                             style="background-color: var(--app-light-pearl);">
                            ${spring.status.value!''}
                        </div>
                    </div>

                    <div>
                        <label for="currentPassword" class="block text-sm font-medium mb-1.5" style="color: var(--app-charcoal);">
                            <@spring.message "changePassword.currentPassword.label"/>
                        </label>
                        <@spring.bind "changePasswordForm.currentPassword"/>
                        <input type="password" id="currentPassword" name="currentPassword"
                               autocomplete="current-password" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-app w-full rounded-lg border px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <div>
                        <label for="newPassword" class="block text-sm font-medium mb-1.5" style="color: var(--app-charcoal);">
                            <@spring.message "changePassword.newPassword.label"/>
                        </label>
                        <@spring.bind "changePasswordForm.newPassword"/>
                        <input type="password" id="newPassword" name="newPassword"
                               autocomplete="new-password" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-app w-full rounded-lg border px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <div>
                        <label for="confirmPassword" class="block text-sm font-medium mb-1.5" style="color: var(--app-charcoal);">
                            <@spring.message "changePassword.confirmPassword.label"/>
                        </label>
                        <@spring.bind "changePasswordForm.confirmPassword"/>
                        <input type="password" id="confirmPassword" name="confirmPassword"
                               autocomplete="new-password" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-app w-full rounded-lg border px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <button type="submit" id="change-password-submit-btn"
                            data-loading-text="<@spring.message "changePassword.submitting"/>"
                            class="btn-primary w-full rounded-lg text-sm font-medium py-3 mt-2 flex items-center justify-center gap-2">
                        <svg id="change-password-spinner" class="hidden animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg"
                             fill="none" viewBox="0 0 24 24" aria-hidden="true">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                        </svg>
                        <span id="change-password-submit-text"><@spring.message "changePassword.submit"/></span>
                    </button>
                </form>
            </div>
        </div>

        <p class="text-center text-xs text-secondary mt-6"><@spring.message "app.footer"/></p>
    </div>

    <script src="/js/change-password-submit.js"></script>

</body>
</html>
