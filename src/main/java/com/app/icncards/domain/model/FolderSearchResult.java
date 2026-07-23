package com.app.icncards.domain.model;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Resultado de una búsqueda de folder: columnas (en el orden de despliegue del
 * administrador) y filas. El filtrado, ordenamiento y paginación se hacen
 * CLIENT-SIDE sobre estas filas (acotadas por maxHits), por lo que aqui no hay
 * estado de orden/pagina.
 *
 * totalHits/shownHits/truncated alimentan el aviso "primeros N de M": shownHits
 * son las filas realmente traidas (tope maxHits) y totalHits el total real
 * (ODFolder.searchCountHits). Si no se pudo contar, totalHits == shownHits y
 * truncated == false.
 */
@Getter
public class FolderSearchResult {

    private final List<String> columns;
    private final List<FolderSearchRow> rows;
    private final int totalHits;
    private final int shownHits;
    private final boolean truncated;
    /** Aviso de ODWEK (getSearchMessage); cadena vacia si no hay. */
    private final String searchMessage;

    public FolderSearchResult(List<String> columns, List<FolderSearchRow> rows,
                              int totalHits, int shownHits, boolean truncated, String searchMessage) {
        this.columns = Collections.unmodifiableList(columns);
        this.rows = Collections.unmodifiableList(rows);
        this.totalHits = totalHits;
        this.shownHits = shownHits;
        this.truncated = truncated;
        this.searchMessage = searchMessage;
    }
}
