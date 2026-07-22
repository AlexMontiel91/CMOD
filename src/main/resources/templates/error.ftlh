<#import "/spring.ftl" as spring/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><@spring.message "error.title"/> — <@spring.message "app.name"/></title>
    <link rel="stylesheet" href="/css/theme.css">
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen flex items-center justify-center px-4 py-10">

    <div class="w-full max-w-md">
        <div class="surface overflow-hidden">
            <div class="record-strip">
                <span>SIST <span class="rv">ICN·CMOD</span></span>
                <span>ESTADO <span class="rv">ERROR</span></span>
            </div>
            <div class="p-8 sm:p-10 text-center">
                <div class="brand-mark mx-auto mb-5" style="width:44px;height:44px;font-size:20px;">!</div>
                <h1 class="h-display text-xl mb-2"><@spring.message "error.title"/></h1>
                <p class="text-sm text-secondary mb-7">
                    <#-- messageCode viene del GlobalExceptionHandler; nunca se muestra el detalle tecnico -->
                    <@spring.message (messageCode!"error.unexpected")/>
                </p>
                <a href="/" class="btn-primary inline-block text-sm px-6 py-2.5">
                    <@spring.message "error.back"/>
                </a>
            </div>
        </div>
    </div>

</body>
</html>
