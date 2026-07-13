<#import "/spring.ftl" as spring/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><@spring.message "home.pageTitle"/> — <@spring.message "app.name"/></title>
    <link rel="stylesheet" href="/css/theme.css">
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen" data-idle-timeout-minutes="${idleTimeoutMinutes!0}">

    <header class="bg-white border-b" style="border-color: var(--app-silver);">
        <div class="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
            <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-lg flex items-center justify-center"
                     style="background-color: var(--app-red);">
                    <span class="text-white font-bold text-xs"><@spring.message "app.logoInitials"/></span>
                </div>
                <span class="font-semibold" style="color: var(--app-black);"><@spring.message "app.name"/></span>
            </div>
            <form method="post" action="/logout">
                <#if _csrf??>
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                </#if>
                <button type="submit" class="text-sm text-secondary hover:opacity-80 transition">
                    <@spring.message "home.logout"/>
                </button>
            </form>
        </div>
    </header>

    <main class="max-w-5xl mx-auto px-6 py-12">
        <h1 class="text-2xl font-bold tracking-tight" style="color: var(--app-black);">
            <@spring.message "home.welcome"/>, ${username!""}
        </h1>

        <#if lastLogonFormatted?? || (firstLogin?? && firstLogin) || (loginInfo?? && loginInfo.failedLoginsSinceLastSuccess gt 0)>
            <p class="text-secondary text-sm mt-1">
                <#if lastLogonFormatted??>
                    <@spring.message "home.lastLogin.label"/>: ${lastLogonFormatted}
                <#elseif firstLogin?? && firstLogin>
                    <@spring.message "home.firstLogin"/>
                </#if>
                <#if loginInfo?? && loginInfo.failedLoginsSinceLastSuccess gt 0>
                    <#if lastLogonFormatted?? || (firstLogin?? && firstLogin)>&nbsp;&middot;&nbsp;</#if>
                    <@spring.message "home.failedLogins.label"/>: ${loginInfo.failedLoginsSinceLastSuccess}
                </#if>
            </p>
        </#if>

        <p class="text-secondary mt-2 mb-8"><@spring.message "home.session.ok"/></p>

        <#if passwordExpiringInDays??>
            <div class="alert-warning rounded-lg px-4 py-3 text-sm mb-6">
                <@spring.message "home.password.expiring.prefix"/> ${passwordExpiringInDays}
                <@spring.message "home.password.expiring.suffix"/>
            </div>
        </#if>

        <#-- Error tecnico al cargar folders -->
        <#if folderError?? && folderError>
            <div class="alert-error rounded-lg px-4 py-3 text-sm mb-6">
                <@spring.message "home.folders.error"/>
            </div>
        </#if>

        <h2 class="text-sm font-semibold uppercase tracking-wide text-secondary mb-3">
            <@spring.message "home.folders.title"/>
        </h2>

        <#if folders?? && folders?size gt 0>
            <div class="mb-4">
                <label for="folder-search" class="sr-only"><@spring.message "home.folders.search.label"/></label>
                <input type="search" id="folder-search" autocomplete="off"
                       placeholder="<@spring.message "home.folders.search.placeholder"/>"
                       class="input-app w-full max-w-sm rounded-lg border px-3.5 py-2.5 text-sm"/>
            </div>

            <div id="folder-table-wrapper"
                 class="bg-white rounded-2xl shadow-sm overflow-hidden border"
                 style="border-color: var(--app-silver);"
                 data-page-size="10"
                 data-info-template="<@spring.message "home.folders.pagination.info"/>">
                <table class="w-full text-sm">
                    <thead>
                        <tr class="text-left" style="background-color: var(--app-light-pearl);">
                            <th class="px-5 py-3 font-medium text-secondary" style="width: 30%;">
                                <@spring.message "home.folders.column.name"/>
                            </th>
                            <th class="px-5 py-3 font-medium text-secondary">
                                <@spring.message "home.folders.column.description"/>
                            </th>
                        </tr>
                    </thead>
                    <tbody id="folder-table-body">
                        <#list folders as folder>
                            <tr class="border-t" style="border-color: var(--app-silver);"
                                data-search="${(folder.name + ' ' + folder.description!'')?lower_case}">
                                <td class="px-5 py-3">
                                    <a href="/folder/${folder.name?url}"
                                       class="font-medium hover:underline"
                                       style="color: var(--app-red);">
                                        ${folder.name}
                                    </a>
                                </td>
                                <td class="px-5 py-3" style="color: var(--app-charcoal);">
                                    ${folder.description!""}
                                </td>
                            </tr>
                        </#list>
                        <tr id="folder-no-results" class="hidden">
                            <td colspan="2" class="px-5 py-6 text-center text-secondary">
                                <@spring.message "home.folders.search.empty"/>
                            </td>
                        </tr>
                    </tbody>
                </table>

                <div class="flex items-center justify-between px-5 py-3 border-t"
                     style="border-color: var(--app-silver);">
                    <span id="folder-pagination-info" class="text-xs text-secondary"></span>
                    <div class="flex items-center gap-2">
                        <button type="button" id="folder-prev"
                                class="px-3 py-1.5 rounded-lg border text-xs font-medium transition"
                                style="border-color: var(--app-silver); color: var(--app-charcoal);">
                            <@spring.message "home.folders.pagination.prev"/>
                        </button>
                        <button type="button" id="folder-next"
                                class="px-3 py-1.5 rounded-lg border text-xs font-medium transition"
                                style="border-color: var(--app-silver); color: var(--app-charcoal);">
                            <@spring.message "home.folders.pagination.next"/>
                        </button>
                    </div>
                </div>
            </div>

            <script src="/js/folder-table.js"></script>
        <#elseif !(folderError?? && folderError)>
            <div class="alert-info rounded-lg px-4 py-3 text-sm">
                <@spring.message "home.folders.empty"/>
            </div>
        </#if>
    </main>

    <#-- Form oculto para el logout por inactividad (idle-timeout.js). CSRF-protegido. -->
    <form id="idle-logout-form" method="post" action="/logout?reason=idle" class="hidden">
        <#if _csrf??>
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        </#if>
    </form>
    <script src="/js/idle-timeout.js"></script>

</body>
</html>
