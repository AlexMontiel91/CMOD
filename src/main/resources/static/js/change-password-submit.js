/**
 * Bloquea el boton de submit del cambio de contrasena y muestra "Actualizando..."
 * mientras se procesa (mismo motivo que en login: el logon de 4 argumentos cruza
 * al mainframe y puede tardar). Ver login-submit.js para el razonamiento completo;
 * se mantiene como archivo propio (no genérico) para no acoplar ambas pantallas.
 */
(function () {
    'use strict';

    function init() {
        var form = document.querySelector('form[action="/change-password"]');
        var btn = document.getElementById('change-password-submit-btn');
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
