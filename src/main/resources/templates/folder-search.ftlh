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

    <main class="max-w-3xl mx-auto px-6 py-10">
        <a href="<@spring.url '/'/>" class="link">&larr; <@spring.message "folder.search.back"/></a>

        <h1 class="h-display text-2xl mt-4">${folderTitle}</h1>
        <#if folderDesc?has_content && folderDesc != folderTitle>
            <p class="text-secondary text-sm mt-1 mb-8">${folderDesc}</p>
        <#else>
            <div class="mb-8"></div>
        </#if>

        <#if submitted?? && submitted>
            <div class="alert alert-info px-4 py-3 text-sm mb-6">
                <@spring.message "folder.search.submitted.info"/>
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

            <#if hasRequired>
                <p class="text-xs text-secondary mb-4">
                    <@spring.message "folder.search.required.legend"/>
                </p>
            </#if>

            <form id="folder-search-form" method="post"
                  action="<@spring.url '/folder/${definition.folderName?url}/search'/>"
                  data-two-value-operators="${twoValueOperators!''}"
                  class="surface p-6 sm:p-8">

                <@ui.csrf/>

                <#-- Encabezado tipo tabla (solo visible en pantallas >= sm) -->
                <div class="hidden sm:grid sm:grid-cols-[1fr_11rem_1fr] gap-4 pb-3 mb-1 border-b"
                     style="border-color: var(--app-line);">
                    <span class="app-label" style="margin-bottom:0;">
                        <@spring.message "folder.search.column.field"/>
                    </span>
                    <span class="app-label" style="margin-bottom:0;">
                        <@spring.message "folder.search.column.operator"/>
                    </span>
                    <span class="app-label" style="margin-bottom:0;">
                        <@spring.message "folder.search.column.value"/>
                    </span>
                </div>

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

                    <div class="grid grid-cols-1 sm:grid-cols-[1fr_11rem_1fr] gap-2 sm:gap-4 items-start
                                py-4 border-b last:border-0"
                         style="border-color: var(--app-line);"
                         data-field-kind="${field.kind.name()}">

                        <#-- Columna 1: nombre del campo.
                             ODCriteria.getName() (nombre del criterio) es el rotulo fiable;
                             getDescription() suele venir vacio. OJO: el operador ! de FreeMarker
                             NO cae al alterno con cadena vacia "", solo con null -> por eso el
                             rotulo se elige explicitamente con ?has_content (name, y si viniera
                             vacio, description). -->
                        <#assign fieldLabel = (field.name)!''>
                        <#if !fieldLabel?has_content><#assign fieldLabel = (field.description)!''></#if>
                        <label class="text-sm font-medium sm:pt-2" style="color: var(--app-ink);">
                            ${fieldLabel}
                            <#if field.required><span style="color: var(--app-error);"> *</span></#if>
                        </label>

                        <#-- Columna 2: operador -->
                        <div>
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
                        <div>
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

                <div class="flex items-center gap-3 mt-6">
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
