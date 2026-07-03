/**
 * Temporizador de inactividad. Protege pantallas desatendidas (equipos de
 * sucursal): tras N minutos sin actividad del usuario, cierra la sesion via POST
 * a /logout (CSRF-protegido) y el servidor redirige a /login.
 *
 * Esto es UX / defensa en profundidad, NO el control de seguridad real. La
 * seguridad real es el invalidationTimeout de <httpSession> en server.xml de
 * Liberty, que expira la sesion en el servidor sin depender de que este script
 * se ejecute. Si el usuario desactiva JS, la sesion sigue viva en el navegador
 * hasta que el servidor la expire por su cuenta.
 *
 * Requiere en la pagina:
 *   - <body data-idle-timeout-minutes="N">
 *   - <form id="idle-logout-form" method="post" action="/logout?reason=idle">
 *       con el input hidden de CSRF
 */
(function () {
    'use strict';

    function initIdleTimeout() {
        var body = document.body;
        var minutes = parseInt(body.getAttribute('data-idle-timeout-minutes'), 10);
        if (!minutes || minutes <= 0) {
            return; // no configurado: no-op
        }

        var timeoutMs = minutes * 60 * 1000;
        var timer = null;

        function doIdleLogout() {
            var form = document.getElementById('idle-logout-form');
            if (form) {
                form.submit();
            }
        }

        function resetTimer() {
            if (timer) {
                clearTimeout(timer);
            }
            timer = setTimeout(doIdleLogout, timeoutMs);
        }

        ['mousemove', 'keydown', 'scroll', 'click', 'touchstart'].forEach(function (evt) {
            document.addEventListener(evt, resetTimer, { passive: true });
        });

        resetTimer();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initIdleTimeout);
    } else {
        initIdleTimeout();
    }
})();
