<#import "/spring.ftl" as spring/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inicio — IMSS</title>
    <link rel="stylesheet" href="/css/theme.css">
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen">

    <header class="bg-white border-b" style="border-color: var(--imss-silver);">
        <div class="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
            <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-lg flex items-center justify-center"
                     style="background-color: var(--imss-red);">
                    <span class="text-white font-bold text-xs">IM</span>
                </div>
                <span class="font-semibold" style="color: var(--imss-black);"><@spring.message "home.brand"/></span>
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
        <h1 class="text-2xl font-bold tracking-tight" style="color: var(--imss-black);">
            <@spring.message "home.welcome"/>, ${username!""}
        </h1>
        <p class="text-secondary mt-2"><@spring.message "home.session.ok"/></p>
    </main>

</body>
</html>
