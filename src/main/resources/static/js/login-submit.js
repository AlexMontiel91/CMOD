/**
 * Bloquea el boton de submit del login y muestra "Iniciando sesion..." mientras
 * se procesa. El logon real cruza region hacia el mainframe y puede tardar; sin
 * esto, el usuario no ve ninguna senal de que ya se esta procesando y puede dar
 * clic varias veces.
 *
 * Se escucha el evento 'submit' del FORM (no el 'click' del boton), para cubrir
 * tanto el clic como el envio con Enter. Deshabilitar el boton DESPUES de que el
 * evento submit ya se disparo no cancela el envio: el navegador ya decidio
 * mandar el formulario, asi que es seguro.
 *
 * Progressive enhancement: si el script no carga, el formulario funciona igual,
 * solo sin el indicador visual. Un doble clic sin este script en el peor caso
 * desperdicia una llamada al mainframe; no representa un riesgo de seguridad
 * (el bloqueo de intentos del backend sigue aplicando igual).
 */
(function () {
    'use strict';

    function initLoginSubmit() {
        var form = document.querySelector('form[action="/login"]');
        var btn = document.getElementById('login-submit-btn');
        var textEl = document.getElementById('login-submit-text');
        var spinner = document.getElementById('login-spinner');
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
        document.addEventListener('DOMContentLoaded', initLoginSubmit);
    } else {
        initLoginSubmit();
    }
})();
