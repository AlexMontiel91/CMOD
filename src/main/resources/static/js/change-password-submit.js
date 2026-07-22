/**
 * Bloquea el boton de submit del cambio de contrasena y muestra "Actualizando..."
 * mientras se procesa (mismo motivo que en login: el logon de 4 argumentos cruza
 * al mainframe y puede tardar). Ver login-submit.js para el razonamiento completo;
 * se mantiene como archivo propio (no genérico) para no acoplar ambas pantallas.
 */
(function () {
    'use strict';

    function init() {
        // El form se obtiene desde el boton (btn.form), no por action="/change-password":
        // asi la ruta puede generarse con <@spring.url> (context root) sin acoplar el
        // selector a la ruta literal.
        var btn = document.getElementById('change-password-submit-btn');
        var form = btn ? btn.form : null;
        var textEl = document.getElementById('change-password-submit-text');
        var spinner = document.getElementById('change-password-spinner');
        if (!form || !btn) {
            return;
        }

        form.addEventListener('submit', function () {
            btn.disabled = true;
            btn.setAttribute('aria-busy', 'true');

            var loadingText = btn.getAttribute('data-loading-text');
            if (textEl && loadingText) {
                textEl.textContent = loadingText;
            }
            if (spinner) {
                spinner.classList.remove('hidden');
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
