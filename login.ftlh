<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Acceso — IMSS</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen flex items-center justify-center bg-slate-100">

    <div class="w-full max-w-sm bg-white rounded-2xl shadow-md p-8">
        <h1 class="text-xl font-semibold text-slate-800 mb-1">Acceso al sistema</h1>
        <p class="text-sm text-slate-500 mb-6">Ingrese con sus credenciales RACF.</p>

        <#if error>
            <div class="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
                Usuario o contraseña incorrectos, o el servicio no está disponible.
            </div>
        </#if>
        <#if logout>
            <div class="mb-4 rounded-lg bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700">
                Su sesión se cerró correctamente.
            </div>
        </#if>

        <form method="post" action="/login" class="space-y-4">
            <#-- token CSRF (requiere spring.freemarker.expose-request-attributes=true) -->
            <#if _csrf??>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            </#if>

            <div>
                <label for="username" class="block text-sm font-medium text-slate-700 mb-1">Usuario</label>
                <input type="text" id="username" name="username" required autofocus
                       class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"/>
            </div>

            <div>
                <label for="password" class="block text-sm font-medium text-slate-700 mb-1">Contraseña</label>
                <input type="password" id="password" name="password" required
                       class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"/>
            </div>

            <button type="submit"
                    class="w-full rounded-lg bg-slate-800 text-white text-sm font-medium py-2.5 hover:bg-slate-700 transition">
                Ingresar
            </button>
        </form>
    </div>

</body>
</html>
