<#import "/spring.ftl" as spring/>
<#import "_layout.ftlh" as ui/>
<#assign pageTitle><@spring.message "home.pageTitle"/></#assign>
<@ui.page title=pageTitle bodyClass="min-h-screen" idle=true>

    <@ui.appHeader/>

    <main class="max-w-7xl mx-auto px-6 py-12">
        <h1 class="h-display text-2xl mb-8"><@spring.message "home.session.ok"/></h1>

        <#if passwordExpiringInDays??>
            <div class="alert alert-warning px-4 py-3 text-sm mb-6">
                <@spring.message "home.password.expiring.prefix"/> ${passwordExpiringInDays}
                <@spring.message "home.password.expiring.suffix"/>
            </div>
        </#if>

        <#-- Error tecnico al cargar folders -->
        <#if folderError?? && folderError>
            <div class="alert alert-error px-4 py-3 text-sm mb-6">
                <@spring.message "home.folders.error"/>
            </div>
        </#if>

        <div class="flex items-baseline justify-between gap-4 mb-3">
            <h2 class="sec-label"><@spring.message "home.folders.title"/></h2>
            <#if folders?? && folders?size gt 0>
                <span class="pager-info">${folders?size} folders</span>
            </#if>
        </div>

        <#if folders?? && folders?size gt 0>
            <div class="search-wrap mb-4">
                <span class="search-ic" aria-hidden="true">⌕</span>
                <label for="folder-search" class="sr-only"><@spring.message "home.folders.search.label"/></label>
                <input type="search" id="folder-search" autocomplete="off"
                       placeholder="<@spring.message "home.folders.search.placeholder"/>"
                       class="input-app w-full px-3.5 py-2.5 text-sm"/>
            </div>

            <div id="folder-table-wrapper"
                 class="folder-index"
                 data-page-size="10"
                 data-info-template="<@spring.message "home.folders.pagination.info"/>">
                <div id="folder-table-body">
                    <#list folders as folder>
                        <a class="idxrow" href="<@spring.url '/folder/${folder.name?url}'/>"
                           data-search="${(folder.name + ' ' + folder.description!'')?lower_case}">
                            <span class="idxn">${(folder?index + 1)?string("00")}</span>
                            <span class="min-w-0">
                                <#-- Alineado con folder-search: nombre (getName) como titulo,
                                     descripcion (getDescription) como secundaria solo si trae contenido. -->
                                <span class="fname block">${folder.name}</span>
                                <#if folder.description?? && folder.description?has_content>
                                    <span class="fdesc block">${folder.description}</span>
                                </#if>
                            </span>
                            <span class="chev" aria-hidden="true">›</span>
                        </a>
                    </#list>
                    <div id="folder-no-results" class="hidden px-5 py-6 text-center text-secondary text-sm">
                        <@spring.message "home.folders.search.empty"/>
                    </div>
                </div>

                <div class="flex items-center justify-between px-4 py-3 border-t" style="border-color: var(--app-line);">
                    <span id="folder-pagination-info" class="pager-info"></span>
                    <div class="flex items-center gap-2">
                        <button type="button" id="folder-prev" class="btn-pager">
                            <@spring.message "home.folders.pagination.prev"/>
                        </button>
                        <button type="button" id="folder-next" class="btn-pager">
                            <@spring.message "home.folders.pagination.next"/>
                        </button>
                    </div>
                </div>
            </div>

            <script src="<@spring.url '/js/folder-table.js'/>"></script>
        <#elseif !(folderError?? && folderError)>
            <div class="alert alert-info px-4 py-3 text-sm">
                <@spring.message "home.folders.empty"/>
            </div>
        </#if>
    </main>

</@ui.page>
