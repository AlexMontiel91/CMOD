package com.app.icncards.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Un criterio de búsqueda que el usuario efectivamente lleno: campo + operador +
 * 1 o 2 valores. Los campos vacios NO producen criterio (no filtran). value2 solo
 * aplica a operadores de dos valores (BETWEEN/NOT_BETWEEN). Sin dependencias de
 * ODWEK: es el input del puerto, el adaptador lo traduce a ODCriteria.
 */
@Getter
@RequiredArgsConstructor
public class FolderSearchCriterion {
    private final String fieldName;
    private final SearchOperator operator;
    private final String value1;
    private final String value2;
}
