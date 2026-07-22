<#import "/spring.ftl" as spring/>
<#import "_layout.ftlh" as ui/>
<#assign pageTitle><@spring.message "changePassword.title"/></#assign>
<@ui.page title=pageTitle noindex=true bodyClass="min-h-screen flex items-center justify-center px-4 py-10">

    <div class="w-full max-w-md">
        <div class="surface overflow-hidden">
            <@ui.recordStrip rightLabel="MODO" rightValue="CAMBIO"/>

            <div class="p-8 sm:p-10">
                <div class="mb-6">
                    <@ui.brand cls="mb-5"/>
                    <h1 class="h-display text-2xl"><@spring.message "changePassword.title"/></h1>
                </div>

                <#-- Explica POR QUE llego aqui (no pudo autenticar con normalidad) -->
                <div class="alert alert-warning mb-6 px-4 py-3 text-sm">
                    <@spring.message "changePassword.reason"/>
                </div>

                <@spring.bind "changePasswordForm"/>
                <#if spring.status.errorMessages?size gt 0>
                    <div class="alert alert-error mb-5 px-4 py-3 text-sm">
                        <#list spring.status.errorMessages as msg>
                            <div>${msg}</div>
                        </#list>
                    </div>
                </#if>

                <form method="post" action="<@spring.url '/change-password'/>" autocomplete="off" novalidate class="space-y-5">
                    <@ui.csrf/>

                    <@spring.bind "changePasswordForm.user"/>
                    <input type="hidden" name="user" value="${spring.status.value!''}"/>
                    <div>
                        <label class="app-label"><@spring.message "login.username.label"/></label>
                        <div class="input-app w-full px-3.5 py-2.5 text-sm text-secondary"
                             style="background-color: var(--app-ledger);">
                            ${spring.status.value!''}
                        </div>
                    </div>

                    <div>
                        <label for="currentPassword" class="app-label">
                            <@spring.message "changePassword.currentPassword.label"/>
                        </label>
                        <@spring.bind "changePasswordForm.currentPassword"/>
                        <input type="password" id="currentPassword" name="currentPassword"
                               autocomplete="current-password" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-app w-full px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <div>
                        <label for="newPassword" class="app-label">
                            <@spring.message "changePassword.newPassword.label"/>
                        </label>
                        <@spring.bind "changePasswordForm.newPassword"/>
                        <input type="password" id="newPassword" name="newPassword"
                               autocomplete="new-password" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-app w-full px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <div>
                        <label for="confirmPassword" class="app-label">
                            <@spring.message "changePassword.confirmPassword.label"/>
                        </label>
                        <@spring.bind "changePasswordForm.confirmPassword"/>
                        <input type="password" id="confirmPassword" name="confirmPassword"
                               autocomplete="new-password" autocapitalize="off" autocorrect="off" spellcheck="false"
                               class="input-app w-full px-3.5 py-2.5 text-sm
                                      <#if spring.status.errorMessages?size gt 0>has-error</#if>"/>
                        <#list spring.status.errorMessages as msg>
                            <p class="field-error mt-1.5 text-xs">${msg}</p>
                        </#list>
                    </div>

                    <button type="submit" id="change-password-submit-btn"
                            data-loading-text="<@spring.message "changePassword.submitting"/>"
                            class="btn-primary w-full text-sm py-3 mt-2 flex items-center justify-center gap-2">
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

        <@ui.footer/>
    </div>

    <script src="<@spring.url '/js/change-password-submit.js'/>"></script>
</@ui.page>
