<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inicio — IMSS</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen bg-slate-100">

    <header class="bg-white border-b border-slate-200">
        <div class="max-w-4xl mx-auto px-6 py-4 flex items-center justify-between">
            <span class="font-semibold text-slate-800">backend-imss</span>
            <form method="post" action="/logout">
                <#if _csrf??>
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                </#if>
                <button type="submit" class="text-sm text-slate-600 hover:text-slate-900">Cerrar sesión</button>
            </form>
        </div>
    </header>

    <main class="max-w-4xl mx-auto px-6 py-10">
        <h1 class="text-2xl font-semibold text-slate-800">Bienvenido, ${username!""}</h1>
        <p class="text-slate-500 mt-2">Sesión iniciada correctamente contra OnDemand.</p>
    </main>

</body>
</html>
