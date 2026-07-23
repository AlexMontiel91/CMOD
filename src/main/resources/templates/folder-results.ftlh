<#import "/spring.ftl" as spring/>
<#import "_layout.ftlh" as ui/>
<#assign folderTitle = (definition.folderName)!''>
<#if !folderTitle?has_content><#assign folderTitle = (definition.folderDescription)!''></#if>
<#assign folderDesc = (definition.folderDescription)!''>
<@ui.page title=folderTitle bodyClass="min-h-screen" idle=true>

    <@ui.appHeader/>

    <main class="max-w-7xl mx-auto px-6 py-10">
        <div class="flex items-start justify-between gap-4 mb-6">
            <a href="<@spring.url '/folder/${definition.folderName?url}'/>" class="link" data-loading style="padding-top:6px;">
                &larr; <@spring.message "folder.results.back"/>
            </a>
            <div class="text-right min-w-0">
                <h1 class="h-display text-2xl">${folderTitle}</h1>
                <#if folderDesc?has_content && folderDesc != folderTitle>
                    <p class="text-secondary text-sm mt-1">${folderDesc}</p>
                </#if>
            </div>
        </div>

        <#-- Resumen: conteo + (si aplica) aviso de límite con tooltip de ayuda -->
        <div class="flex items-center justify-between gap-3 flex-wrap mb-3">
            <span class="pager-info">
                <#if result.truncated>
                    <@spring.message "folder.results.showingFirst"/> ${result.shownHits}
                    <@spring.message "folder.results.of"/> ${result.totalHits}
                    <@spring.message "folder.results.matches"/>
                <#else>
                    ${result.totalHits} <@spring.message "folder.results.total"/>
                </#if>
            </span>
            <#if result.truncated>
                <span class="hint" tabindex="0" role="note">
                    <@spring.message "folder.results.limit.label"/>
                    <span class="hint-ic" aria-hidden="true">?</span>
                    <span class="hint-tip" role="tooltip"><@spring.message "folder.results.limit.help"/></span>
                </span>
            </#if>
        </div>

        <#-- Aviso informativo del servidor (getSearchMessage), si lo hubo -->
        <#if result.searchMessage?has_content>
            <div class="alert alert-info px-4 py-3 text-sm mb-4">${result.searchMessage}</div>
        </#if>

        <#if result.rows?size == 0>
            <div class="alert alert-info px-4 py-3 text-sm">
                <@spring.message "folder.results.empty"/>
            </div>
        <#else>
            <div class="search-wrap mb-4">
                <span class="search-ic" aria-hidden="true">⌕</span>
                <label for="results-search" class="sr-only"><@spring.message "folder.results.filter.placeholder"/></label>
                <input type="search" id="results-search" autocomplete="off"
                       placeholder="<@spring.message "folder.results.filter.placeholder"/>"
                       class="input-app w-full px-3.5 py-2.5 text-sm"/>
            </div>

            <div id="results-wrapper"
                 data-page-size="10"
                 data-info-template="<@spring.message "home.folders.pagination.info"/>">
                <#-- La tabla scrollea horizontalmente en su propio contenedor -->
                <div class="rt-wrap">
                    <table class="rt" id="results-table">
                        <thead>
                            <tr>
                                <th class="rt-idx">#</th>
                                <#list result.columns as col>
                                    <th class="rt-sortable" data-col="${col?index}" role="button" tabindex="0">
                                        <span class="rt-th-label">${col}</span><span class="rt-arrow" aria-hidden="true"></span>
                                    </th>
                                </#list>
                                <th class="rt-dl"><@spring.message "folder.results.column.download"/></th>
                            </tr>
                        </thead>
                        <tbody id="results-body">
                            <#list result.rows as row>
                                <tr data-search="${row.values?join(' ')?lower_case}" data-docid="${row.docId}">
                                    <td class="rt-idx"></td>
                                    <#list row.values as v>
                                        <td><span class="rt-cell" title="${v}">${v}</span></td>
                                    </#list>
                                    <td class="rt-dl">
                                        <#-- Placeholder: la descarga (con el docId de la fila) es el siguiente incremento -->
                                        <span class="dl-btn" title="<@spring.message "folder.results.download.pending"/>" aria-disabled="true">&darr;</span>
                                    </td>
                                </tr>
                            </#list>
                            <tr id="results-no-results" class="hidden">
                                <td colspan="999" class="px-5 py-6 text-center text-secondary text-sm">
                                    <@spring.message "folder.results.filter.empty"/>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div class="flex items-center justify-between px-1 py-3 mt-1">
                    <span id="results-pagination-info" class="pager-info"></span>
                    <div class="flex items-center gap-2">
                        <button type="button" id="results-prev" class="btn-pager">
                            <@spring.message "home.folders.pagination.prev"/>
                        </button>
                        <button type="button" id="results-next" class="btn-pager">
                            <@spring.message "home.folders.pagination.next"/>
                        </button>
                    </div>
                </div>
            </div>

            <script src="<@spring.url '/js/results-table.js'/>"></script>
        </#if>
    </main>

</@ui.page>
