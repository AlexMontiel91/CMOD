/**
 * Tabla de resultados de búsqueda: filtrado, ordenamiento y paginación CLIENT-SIDE
 * sobre las filas ya renderizadas (≤ maxHits). No hace llamadas nuevas.
 *
 * Por qué client-side: ODWEK devuelve todos los hits (hasta maxHits) en una sola
 * búsqueda; ordenar en el servidor tiene overhead y con maxHits<total no da el
 * "verdadero top N" (ver Redbook cap. 8). Sobre unos cientos de filas, ordenar y
 * paginar en el navegador es exacto e instantáneo.
 *
 * Todas las columnas son ordenables (clic en el <th>); la detección de columna
 * numérica es automática para no ordenar números como texto. Progressive
 * enhancement: sin JS, la tabla se ve completa, solo sin filtro/orden/paginación.
 *
 * Requiere en la página:
 *   - #results-wrapper con data-page-size y data-info-template ({from} {to} {total})
 *   - #results-table con #results-body; filas <tr data-search="..."> y celdas:
 *       td[0] = índice (.rt-idx), td[1..N] = valores, td[último] = descarga.
 *   - <th class="rt-sortable" data-col="i"> con un <span class="rt-arrow">
 *   - #results-search, #results-prev, #results-next, #results-pagination-info,
 *     #results-no-results (opcionales)
 */
(function () {
    'use strict';

    // Valor "numérico" reconocible (moneda, miles, decimales, %); NO cadenas con
    // letras/guiones internos (folios, NSS) ni fechas ISO.
    var NUM_RE = /^-?\$?\s*[\d,]+(\.\d+)?\s*%?$/;

    function normalize(text) {
        return (text || '').toString()
            .normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
    }

    function pad2(n) {
        return (n < 10 ? '0' : '') + n;
    }

    function parseNum(v) {
        return parseFloat(v.replace(/[^0-9.\-]/g, ''));
    }

    function init() {
        var wrapper = document.getElementById('results-wrapper');
        var tbody = document.getElementById('results-body');
        if (!wrapper || !tbody) {
            return;
        }

        var pageSize = parseInt(wrapper.getAttribute('data-page-size'), 10) || 10;
        var infoTemplate = wrapper.getAttribute('data-info-template') || 'Mostrando {from}–{to} de {total}';

        var allRows = Array.prototype.slice.call(tbody.querySelectorAll('tr[data-search]'));
        var noResultsRow = document.getElementById('results-no-results');
        var searchInput = document.getElementById('results-search');
        var infoEl = document.getElementById('results-pagination-info');
        var prevBtn = document.getElementById('results-prev');
        var nextBtn = document.getElementById('results-next');
        var sortableThs = Array.prototype.slice.call(
            document.querySelectorAll('#results-table thead th.rt-sortable'));

        var query = '';
        var page = 1;
        var sortCol = null;   // índice de columna de datos (data-col), o null
        var sortDir = 1;      // 1 asc, -1 desc

        function cellValue(row, col) {
            var cell = row.cells[col + 1]; // +1: la celda 0 es el índice (#)
            return cell ? cell.textContent.trim() : '';
        }

        function matchingRows() {
            if (!query) {
                return allRows.slice();
            }
            return allRows.filter(function (row) {
                return normalize(row.getAttribute('data-search')).indexOf(query) !== -1;
            });
        }

        function isNumericColumn(rows, col) {
            var seen = false;
            for (var i = 0; i < rows.length; i++) {
                var v = cellValue(rows[i], col);
                if (v === '') { continue; }
                seen = true;
                if (!NUM_RE.test(v)) { return false; }
            }
            return seen; // numérica solo si hubo valores y todos son numéricos
        }

        function sortRows(rows) {
            if (sortCol === null) {
                return rows;
            }
            var numeric = isNumericColumn(rows, sortCol);
            var copy = rows.slice();
            copy.sort(function (a, b) {
                var va = cellValue(a, sortCol);
                var vb = cellValue(b, sortCol);
                var cmp;
                if (numeric) {
                    var na = parseNum(va), nb = parseNum(vb);
                    if (isNaN(na) && isNaN(nb)) { cmp = 0; }
                    else if (isNaN(na)) { cmp = 1; }
                    else if (isNaN(nb)) { cmp = -1; }
                    else { cmp = na - nb; }
                } else {
                    cmp = normalize(va).localeCompare(normalize(vb), 'es');
                }
                return cmp * sortDir;
            });
            return copy;
        }

        function updateArrows() {
            sortableThs.forEach(function (th) {
                var col = parseInt(th.getAttribute('data-col'), 10);
                var arrow = th.querySelector('.rt-arrow');
                var active = (sortCol === col);
                if (arrow) { arrow.textContent = active ? (sortDir === 1 ? ' ▲' : ' ▼') : ''; }
                th.classList.toggle('rt-sorted', active);
            });
        }

        function render() {
            var sorted = sortRows(matchingRows());
            var total = sorted.length;
            var totalPages = Math.max(1, Math.ceil(total / pageSize));
            if (page > totalPages) { page = totalPages; }

            allRows.forEach(function (r) { r.classList.add('hidden'); });

            var start = (page - 1) * pageSize;
            sorted.slice(start, start + pageSize).forEach(function (row, i) {
                tbody.appendChild(row); // reordena las filas visibles al orden actual
                row.classList.remove('hidden');
                var idx = row.querySelector('.rt-idx');
                if (idx) { idx.textContent = pad2(start + i + 1); }
            });

            if (noResultsRow) {
                tbody.appendChild(noResultsRow); // mantener al final
                noResultsRow.classList.toggle('hidden', total !== 0);
            }

            if (infoEl) {
                infoEl.textContent = total === 0 ? '' : infoTemplate
                    .replace('{from}', String(start + 1))
                    .replace('{to}', String(Math.min(start + pageSize, total)))
                    .replace('{total}', String(total));
            }
            if (prevBtn) { prevBtn.disabled = page <= 1; }
            if (nextBtn) { nextBtn.disabled = page >= totalPages; }
            updateArrows();
        }

        if (searchInput) {
            var timer = null;
            searchInput.addEventListener('input', function () {
                clearTimeout(timer);
                timer = setTimeout(function () {
                    query = normalize(searchInput.value.trim());
                    page = 1;
                    render();
                }, 150);
            });
        }
        if (prevBtn) {
            prevBtn.addEventListener('click', function () {
                if (page > 1) { page -= 1; render(); }
            });
        }
        if (nextBtn) {
            nextBtn.addEventListener('click', function () { page += 1; render(); });
        }

        sortableThs.forEach(function (th) {
            var col = parseInt(th.getAttribute('data-col'), 10);
            function toggle() {
                if (sortCol === col) { sortDir = -sortDir; }
                else { sortCol = col; sortDir = 1; }
                page = 1;
                render();
            }
            th.addEventListener('click', toggle);
            th.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggle(); }
            });
        });

        render();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
