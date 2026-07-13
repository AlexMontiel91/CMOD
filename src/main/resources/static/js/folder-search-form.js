/**
 * Formulario dinamico de busqueda de un folder:
 *  1) Muestra/oculta el segundo input (value2) segun el operador elegido en cada
 *     campo. La lista de operadores que requieren dos valores (Entre/No entre)
 *     viene del servidor via data-two-value-operators, para no duplicar la regla
 *     de negocio (SearchOperator.requiresTwoValues()) en JS.
 *  2) Bloquea AMBOS botones (Buscar y Limpiar formulario) al enviar, y muestra
 *     "Buscando..." mientras se procesa (la llamada cruza al mainframe y puede
 *     tardar). Bloquear tambien "Limpiar" evita que el usuario reinicie el
 *     formulario mientras una busqueda ya esta en curso.
 *  3) El reset NATIVO del navegador restaura los VALORES de los inputs pero NO
 *     dispara 'change' en los <select> de operador de forma confiable, asi que
 *     el segundo valor (value2) podria quedar visible/oculto de forma incorrecta
 *     tras dar clic en "Limpiar formulario". Se escucha el evento 'reset' del
 *     form y se re-sincroniza la visibilidad despues de que el navegador aplique
 *     los valores originales.
 *
 * Progressive enhancement: sin este script, todos los campos value1/value2 se ven
 * siempre (visibles), y el formulario sigue siendo funcional.
 */
(function () {
    'use strict';

    function init() {
        var form = document.getElementById('folder-search-form');
        if (!form) {
            return;
        }

        var twoValueOps = (form.getAttribute('data-two-value-operators') || '')
            .split(',')
            .map(function (s) { return s.trim(); })
            .filter(Boolean);

        function applyToggle(select) {
            var block = select.closest('[data-field-kind]');
            if (!block) {
                return;
            }
            var value2 = block.querySelector('.value2-input');
            if (!value2) {
                return;
            }
            var needsTwo = twoValueOps.indexOf(select.value) !== -1;
            value2.classList.toggle('hidden', !needsTwo);
            if (!needsTwo) {
                value2.value = '';
            }
        }

        var selects = Array.prototype.slice.call(form.querySelectorAll('.operator-select'));

        // 1) Toggle al cambiar el operador
        selects.forEach(function (select) {
            select.addEventListener('change', function () {
                applyToggle(select);
            });
        });

        // 2) Bloqueo de ambos botones al enviar
        var submitBtn = document.getElementById('folder-search-submit-btn');
        var submitText = document.getElementById('folder-search-submit-text');
        var resetBtn = document.getElementById('folder-search-reset-btn');

        form.addEventListener('submit', function () {
            if (submitBtn) {
                submitBtn.disabled = true;
                var loadingText = submitBtn.getAttribute('data-loading-text');
                if (submitText && loadingText) {
                    submitText.textContent = loadingText;
                }
            }
            if (resetBtn) {
                resetBtn.disabled = true; // evita "limpiar" con una busqueda en curso
            }
        });

        // 3) Resincroniza value1/value2 despues del reset nativo
        form.addEventListener('reset', function () {
            setTimeout(function () {
                selects.forEach(function (select) {
                    applyToggle(select);
                });
            }, 0);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
