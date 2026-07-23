/**
 * Overlay "Cargando…" para toda acción que cruza a OnDemand (lenta en dev, rápida
 * en prod). Se dispara al hacer clic en cualquier <a data-loading> o al enviar
 * cualquier <form data-loading>; el texto sale de data-loading-text (o "Cargando…").
 * Se retira solo cuando carga la nueva página.
 *
 * Unifica el feedback de navegación: abrir folder (home), "Volver a la búsqueda"
 * (resultados), logo → listado, y el envío del formulario de búsqueda. Los botones
 * de submit además se deshabilitan por sus propios scripts (no reenviar).
 *
 * Progressive enhancement: sin JS, la navegación/envío funciona igual, sin aviso.
 */
(function () {
    'use strict';

    function showOverlay(text) {
        if (document.querySelector('.loading-overlay')) {
            return; // ya visible
        }
        var overlay = document.createElement('div');
        overlay.className = 'loading-overlay';
        var box = document.createElement('div');
        box.className = 'loading-overlay__box';
        var spinner = document.createElement('span');
        spinner.className = 'loading-overlay__spinner';
        var label = document.createElement('span');
        label.textContent = text || 'Cargando…';
        box.appendChild(spinner);
        box.appendChild(label);
        overlay.appendChild(box);
        document.body.appendChild(overlay);
    }

    function init() {
        Array.prototype.forEach.call(document.querySelectorAll('a[data-loading]'), function (link) {
            link.addEventListener('click', function (e) {
                // respeta abrir en pestaña nueva / clic no primario: ahí no navega esta página
                if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey) {
                    return;
                }
                showOverlay(link.getAttribute('data-loading-text'));
            });
        });
        Array.prototype.forEach.call(document.querySelectorAll('form[data-loading]'), function (form) {
            form.addEventListener('submit', function () {
                showOverlay(form.getAttribute('data-loading-text'));
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
