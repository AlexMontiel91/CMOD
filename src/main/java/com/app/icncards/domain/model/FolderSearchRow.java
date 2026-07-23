package com.app.icncards.domain.model;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Una fila de resultados: los valores de despliegue en el orden de las columnas,
 * mas el docId (identificador del documento en OnDemand). El docId NO se muestra
 * al usuario; es la referencia para el enlace de descarga (siguiente incremento).
 */
@Getter
public class FolderSearchRow {

    private final List<String> values;
    private final String docId;

    public FolderSearchRow(List<String> values, String docId) {
        this.values = Collections.unmodifiableList(values);
        this.docId = docId;
    }
}
