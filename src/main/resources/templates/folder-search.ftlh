<#import "/spring.ftl" as spring/>
<#import "_layout.ftlh" as ui/>
<#-- Titulo = nombre del folder (ODFolder.getName, fiable); subtitulo = descripcion
     (getDescription, puede venir vacia). Se blindan con ?has_content porque el operador
     ! de FreeMarker NO cae al alterno con cadena vacia "", solo con null. -->
<#assign folderTitle = (definition.folderName)!''>
<#if !folderTitle?has_content><#assign folderTitle = (definition.folderDescription)!''></#if>
<#assign folderDesc = (definition.folderDescription)!''>
<@ui.page title=folderTitle bodyClass="min-h-screen" idle=true>

    <@ui.appHeader/>

    <main class="max-w-7xl mx-auto px-6 py-10">
        <div class="flex items-start justify-between gap-4 mb-8">
            <a href="<@spring.url '/'/>" class="link" data-loading style="padding-top:6px;">&larr; <@spring.message "folder.search.back"/></a>
            <div class="text-right min-w-0">
                <h1 class="h-display text-2xl">${folderTitle}</h1>
                <#if folderDesc?has_content && folderDesc != folderTitle>
                    <p class="text-secondary text-sm mt-1">${folderDesc}</p>
                </#if>
            </div>
        </div>

        <#if searchError?? && searchError>
            <div class="alert alert-error px-4 py-3 text-sm mb-6">
                <@spring.message "folder.search.error"/>
            </div>
        </#if>

        <#if definition.fields?size == 0>
            <div class="alert alert-info px-4 py-3 text-sm">
                <@spring.message "folder.search.empty"/>
            </div>
        <#else>
            <#-- valores repoblados si el POST anterior fallo por campos obligatorios -->
            <#assign values = submittedValues!{}>
            <#assign twoValueList = (twoValueOperators!'')?split(",")>
            <#assign hasRequired = false>
            <#list definition.fields as f>
                <#if f.required><#assign hasRequired = true></#if>
            </#list>

            <form id="folder-search-form" method="post"
                  action="<@spring.url '/folder/${definition.folderName?url}/search'/>"
                  data-two-value-operators="${twoValueOperators!''}"
                  data-loading data-loading-text="<@spring.message "folder.search.submitting"/>"
                  class="surface p-6 sm:p-8">

                <@ui.csrf/>

                <#if hasRequired>
                    <p class="text-xs text-secondary mb-5">
                        <@spring.message "folder.search.required.legend"/>
                    </p>
                </#if>

                <#-- Un unico grid de 3 columnas para TODO el formulario: cada fila usa
                     display:contents (en >=sm) para que sus 3 celdas se alineen con las
                     demas. En movil el grid se vuelve bloque y cada campo se apila. -->
                <div class="search-grid">
                    <div class="search-grid__head"><@spring.message "folder.search.column.field"/></div>
                    <div class="search-grid__head"><@spring.message "folder.search.column.operator"/></div>
                    <div class="search-grid__head search-grid__head--last"><@spring.message "folder.search.column.value"/></div>

                    <#list definition.fields as field>
                        <#assign opKey = "criteria[" + field.name + "].operator">
                        <#assign v1Key = "criteria[" + field.name + "].value1">
                        <#assign v2Key = "criteria[" + field.name + "].value2">

                        <#assign selectedOp = (values[opKey])!((field.defaultOperator.name())!'')>
                        <#assign v1Value = (values[v1Key])!field.defaultValue1!''>
                        <#assign v2Value = (values[v2Key])!field.defaultValue2!''>
                        <#assign showTwo = twoValueList?seq_contains(selectedOp)>
                        <#assign fieldHasError = missingFields?? && missingFields?seq_contains(field.name)>
                        <#assign errorClass = ""><#if fieldHasError><#assign errorClass = " has-error"></#if>

                        <#-- Rotulo: getName() es el fiable; getDescription() suele venir vacio
                             y el operador ! no cae al alterno con "", por eso se usa ?has_content. -->
                        <#assign fieldLabel = (field.name)!''>
                        <#if !fieldLabel?has_content><#assign fieldLabel = (field.description)!''></#if>

                        <div class="search-grid__row" data-field-kind="${field.kind.name()}">
                            <#-- Columna 1: nombre del campo -->
                            <div class="search-grid__cell">
                                <label class="flabel" style="color: var(--app-ink);">
                                    ${fieldLabel}
                                    <#if field.required><span style="color: var(--app-error);"> *</span></#if>
                                </label>
                            </div>

                            <#-- Columna 2: operador -->
                            <div class="search-grid__cell">
                                <#if field.validOperators?size gt 1>
                                    <select name="${opKey}"
                                            class="operator-select input-app w-full px-3 py-2 text-sm">
                                        <#list field.validOperators as op>
                                            <option value="${op.name()}" <#if op.name() == selectedOp>selected</#if>>
                                                <@spring.message "search.operator.${op.name()}"/>
                                            </option>
                                        </#list>
                                    </select>
                                <#elseif field.validOperators?size == 1>
                                    <input type="hidden" name="${opKey}" value="${field.validOperators[0].name()}"/>
                                    <span class="input-app w-full px-3 py-2 text-sm flex items-center text-secondary">
                                        <@spring.message "search.operator.${field.validOperators[0].name()}"/>
                                    </span>
                                </#if>
                            </div>

                            <#-- Columna 3: valor(es), segun el tipo del campo -->
                            <div class="search-grid__cell search-grid__cell--val">
                                <div class="flex flex-col sm:flex-row gap-2">
                                    <#if field.kind.name() == "DATE">
                                        <input type="date" name="${v1Key}" value="${v1Value}"
                                               <#if field.required>required</#if>
                                               class="value1-input input-app flex-1 px-3 py-2 text-sm${errorClass}"/>
                                        <input type="date" name="${v2Key}" value="${v2Value}"
                                               class="value2-input input-app flex-1 px-3 py-2 text-sm<#if !showTwo> hidden</#if>"/>
                                    <#elseif field.kind.name() == "DATETIME">
                                        <input type="datetime-local" name="${v1Key}" value="${v1Value}"
                                               <#if field.required>required</#if>
                                               class="value1-input input-app flex-1 px-3 py-2 text-sm${errorClass}"/>
                                        <input type="datetime-local" name="${v2Key}" value="${v2Value}"
                                               class="value2-input input-app flex-1 px-3 py-2 text-sm<#if !showTwo> hidden</#if>"/>
                                    <#elseif field.kind.name() == "NUMBER">
                                        <input type="number" name="${v1Key}" value="${v1Value}"
                                               <#if field.required>required</#if>
                                               class="value1-input input-app flex-1 px-3 py-2 text-sm${errorClass}"/>
                                        <input type="number" name="${v2Key}" value="${v2Value}"
                                               class="value2-input input-app flex-1 px-3 py-2 text-sm<#if !showTwo> hidden</#if>"/>
                                    <#elseif field.kind.name() == "CHOICE">
                                        <#-- Nota: solo selector de un valor. Si un campo choice admite el
                                             operador "In" para seleccion multiple, se resolvera cuando haga
                                             falta un caso real; hoy se limita a seleccion unica. -->
                                        <select name="${v1Key}"
                                                <#if field.required>required</#if>
                                                class="value1-input input-app flex-1 px-3 py-2 text-sm${errorClass}">
                                            <option value="">—</option>
                                            <#list field.choiceValues as choice>
                                                <option value="${choice}" <#if choice == v1Value>selected</#if>>
                                                    ${choice}
                                                </option>
                                            </#list>
                                        </select>
                                    <#else>
                                        <input type="text" name="${v1Key}" value="${v1Value}"
                                               <#if field.maxLength gt 0>maxlength="${field.maxLength}"</#if>
                                               <#if field.required>required</#if>
                                               class="value1-input input-app flex-1 px-3 py-2 text-sm${errorClass}"/>
                                        <input type="text" name="${v2Key}" value="${v2Value}"
                                               <#if field.maxLength gt 0>maxlength="${field.maxLength}"</#if>
                                               class="value2-input input-app flex-1 px-3 py-2 text-sm<#if !showTwo> hidden</#if>"/>
                                    </#if>
                                </div>
                                <#if fieldHasError>
                                    <p class="field-error mt-1.5 text-xs">
                                        <@spring.message "folder.search.field.required"/>
                                    </p>
                                </#if>
                            </div>
                        </div>
                    </#list>
                </div>

                <div class="flex items-center justify-end gap-3 mt-6">
                    <button type="submit" id="folder-search-submit-btn"
                            data-loading-text="<@spring.message "folder.search.submitting"/>"
                            class="btn-primary text-sm px-6 py-2.5">
                        <span id="folder-search-submit-text"><@spring.message "folder.search.submit"/></span>
                    </button>
                    <button type="reset" id="folder-search-reset-btn"
                            class="btn-secondary text-sm px-6 py-2.5">
                        <@spring.message "folder.search.reset"/>
                    </button>
                </div>
            </form>
        </#if>
    </main>

    <script src="<@spring.url '/js/folder-search-form.js'/>"></script>
</@ui.page>
