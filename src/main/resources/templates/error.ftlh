<#import "/spring.ftl" as spring/>
<#import "_layout.ftlh" as ui/>
<#assign pageTitle><@spring.message "error.title"/></#assign>
<@ui.page title=pageTitle bodyClass="min-h-screen flex items-center justify-center px-4 py-10">

    <div class="w-full max-w-md">
        <div class="surface overflow-hidden">
            <@ui.recordStrip rightLabel="ESTADO" rightValue="ERROR"/>
            <div class="p-8 sm:p-10 text-center">
                <div class="brand-mark mx-auto mb-5" style="width:44px;height:44px;font-size:20px;">!</div>
                <h1 class="h-display text-xl mb-2"><@spring.message "error.title"/></h1>
                <p class="text-sm text-secondary mb-7">
                    <#-- messageCode viene del GlobalExceptionHandler; nunca se muestra el detalle tecnico -->
                    <@spring.message (messageCode!"error.unexpected")/>
                </p>
                <a href="<@spring.url '/'/>" class="btn-primary inline-block text-sm px-6 py-2.5">
                    <@spring.message "error.back"/>
                </a>
            </div>
        </div>
    </div>

</@ui.page>
