/**
 * Busqueda y paginacion CLIENT-SIDE de la tabla de folders.
 *
 * Por que client-side: ODWEK (getFolderNames) no pagina en el origen, siempre
 * devuelve TODOS los folders del usuario en una sola llamada. El servidor ya
 * renderiza la tabla completa (progressive enhancement: sin este script, la
 * tabla sigue siendo visible e integra, solo sin buscador/paginacion). Este
 * script opera sobre las filas ya presentes en el DOM, sin llamadas nuevas.
 *
 * Requiere en la pagina:
 *   - #folder-table-wrapper con data-page-size y data-info-template
 *       (el template usa los tokens literales {from} {to} {total})
 *   - #folder-table-body con filas (cualquier elemento) con
 *       data-search="texto en minusculas". Cada fila puede incluir un
 *       elemento .idxn cuyo texto se renumera a la posicion visible.
 *   - #folder-no-results (fila a mostrar cuando el filtro no encuentra nada)
 *   - #folder-search (input de texto, opcional)
 *   - #folder-prev / #folder-next / #folder-pagination-info (opcionales)
 */
(function () {
    'use strict';

    function normalize(text) {
        return (text || '')
            .toString()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '') // quita acentos para busqueda mas tolerante
            .toLowerCase();
    }

    function pad2(n) {
        return (n < 10 ? '0' : '') + n;
    }

    // Overlay "Cargando..." al abrir un folder: la navegacion a /folder/{n} dispara
    // una consulta a OnDemand que en dev puede tardar; el overlay le dice al usuario
    // que su solicitud se esta procesando. Se retira solo al cargar la nueva pagina.
    function showLoadingOverlay(text) {
        if (document.querySelector('.loading-overlay')) {
            return;
        }
        var overlay = document.createElement('div');
        overlay.className = 'loading-overlay';
        var box = document.createElement('div');
        box.className = 'loading-overlay__box';
        var spinner = document.createElement('span');
        spinner.className = 'loading-overlay__spinner';
        var label = document.createElement('span');
        label.textContent = text;
        box.appendChild(spinner);
        box.appendChild(label);
        overlay.appendChild(box);
        document.body.appendChild(overlay);
    }

    function initFolderTable() {
        var wrapper = document.getElementById('folder-table-wrapper');
        var body = document.getElementById('folder-table-body');
        if (!wrapper || !body) {
            return;
        }

        var pageSize = parseInt(wrapper.getAttribute('data-page-size'), 10) || 10;
        var infoTemplate = wrapper.getAttribute('data-info-template') || 'Mostrando {from}\u2013{to} de {total}';

        var allRows = Array.prototype.slice.call(body.querySelectorAll('[data-search]'));
        var noResultsRow = document.getElementById('folder-no-results');
        var searchInput = document.getElementById('folder-search');
        var infoEl = document.getElementById('folder-pagination-info');
        var prevBtn = document.getElementById('folder-prev');
        var nextBtn = document.getElementById('folder-next');

        var query = '';
        var page = 1;

        function matchingRows() {
            if (!query) {
                return allRows;
            }
            return allRows.filter(function (row) {
                return normalize(row.getAttribute('data-search')).indexOf(query) !== -1;
            });
        }

        function render() {
            var rows = matchingRows();
            var total = rows.length;
            var totalPages = Math.max(1, Math.ceil(total / pageSize));
            if (page > totalPages) {
                page = totalPages;
            }

            allRows.forEach(function (row) {
                row.classList.add('hidden');
            });

            var start = (page - 1) * pageSize;
            rows.slice(start, start + pageSize).forEach(function (row, i) {
                row.classList.remove('hidden');
                // renumera la fila visible a su posicion en el listado (coincide
                // con "Mostrando X-Y de Z"); si no hay .idxn, no-op
                var numEl = row.querySelector('.idxn');
                if (numEl) {
                    numEl.textContent = pad2(start + i + 1);
                }
            });

            if (noResultsRow) {
                noResultsRow.classList.toggle('hidden', total !== 0);
            }

            if (infoEl) {
                infoEl.textContent = total === 0 ? '' : infoTemplate
                    .replace('{from}', String(start + 1))
                    .replace('{to}', String(Math.min(start + pageSize, total)))
                    .replace('{total}', String(total));
            }

            if (prevBtn) {
                prevBtn.disabled = page <= 1;
                prevBtn.classList.toggle('opacity-40', page <= 1);
                prevBtn.classList.toggle('cursor-not-allowed', page <= 1);
            }
            if (nextBtn) {
                nextBtn.disabled = page >= totalPages;
                nextBtn.classList.toggle('opacity-40', page >= totalPages);
                nextBtn.classList.toggle('cursor-not-allowed', page >= totalPages);
            }
        }

        if (searchInput) {
            var debounceTimer = null;
            searchInput.addEventListener('input', function () {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(function () {
                    query = normalize(searchInput.value.trim());
                    page = 1;
                    render();
                }, 150);
            });
        }

        if (prevBtn) {
            prevBtn.addEventListener('click', function () {
                if (page > 1) {
                    page -= 1;
                    render();
                }
            });
        }
        if (nextBtn) {
            nextBtn.addEventListener('click', function () {
                page += 1;
                render();
            });
        }

        // Aviso "Cargando..." al abrir un folder (navegacion normal del enlace).
        var loadingText = wrapper.getAttribute('data-loading-text') || 'Cargando…';
        allRows.forEach(function (row) {
            row.addEventListener('click', function (e) {
                // respeta abrir en pestana nueva / clic no primario: ahi no navega esta pagina
                if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey) {
                    return;
                }
                showLoadingOverlay(loadingText);
            });
        });

        render();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initFolderTable);
    } else {
        initFolderTable();
    }
})();
