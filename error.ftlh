<#import "/spring.ftl" as spring/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ocurrió un problema — IMSS</title>
    <link rel="stylesheet" href="/css/theme.css">
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen flex items-center justify-center px-4">

    <div class="w-full max-w-md">
        <div class="bg-white rounded-2xl shadow-xl overflow-hidden">
            <div class="accent-bar h-1.5"></div>
            <div class="p-8 sm:p-10 text-center">
                <div class="w-11 h-11 rounded-xl flex items-center justify-center mb-4 mx-auto"
                     style="background-color: var(--imss-red);">
                    <span class="text-white font-bold text-lg">!</span>
                </div>
                <h1 class="text-xl font-bold tracking-tight mb-2" style="color: var(--imss-black);">
                    Ocurrió un problema
                </h1>
                <p class="text-sm text-secondary mb-6">
                    <#-- messageCode viene del GlobalExceptionHandler; nunca se muestra el detalle tecnico -->
                    <@spring.message (messageCode!"error.unexpected")/>
                </p>
                <a href="/" class="btn-imss inline-block rounded-lg text-sm font-medium px-6 py-2.5">
                    Volver al inicio
                </a>
            </div>
        </div>
    </div>

</body>
</html>
