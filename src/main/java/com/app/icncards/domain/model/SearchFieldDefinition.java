package com.app.icncards.domain.model;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Definicion de un campo de busqueda de un folder (name, tipo, operadores validos,
 * valores por defecto, etc.). Sin dependencias de ODWEK: lo que necesita la vista
 * para componer el formulario dinamico, ya traducido del lenguaje de ODCriteria.
 *
 * defaultValue1/defaultValue2: ODCriteria.getSearchValues() devuelve un PAR fijo
 * (no una lista de N valores) que corresponde exactamente a value1/value2 del
 * formulario -- confirmado por el ejemplo oficial de IBM (TcListCriteria), que
 * accede a value_vec[0] y value_vec[1] sin verificar longitud. Por eso ambos se
 * exponen por separado, no como lista.
 */
@Getter
public class SearchFieldDefinition {

    private final String name;
    private final String description;
    private final SearchFieldKind kind;
    private final boolean required;
    private final SearchOperator defaultOperator;
    private final List<SearchOperator> validOperators;
    private final String defaultValue1;
    private final String defaultValue2;
    private final List<String> choiceValues;
    private final int maxLength;
    /** Formato esperado por OnDemand para el valor de busqueda (solo DATE/DATETIME), p. ej. "%m/%d/%y". */
    private final String dateFormat;

    public SearchFieldDefinition(String name, String description, SearchFieldKind kind,
                                 boolean required, SearchOperator defaultOperator,
                                 List<SearchOperator> validOperators, String defaultValue1,
                                 String defaultValue2, List<String> choiceValues,
                                 int maxLength, String dateFormat) {
        this.name = name;
        this.description = description;
        this.kind = kind;
        this.required = required;
        this.defaultOperator = defaultOperator;
        this.validOperators = Collections.unmodifiableList(validOperators);
        this.defaultValue1 = defaultValue1;
        this.defaultValue2 = defaultValue2;
        this.choiceValues = Collections.unmodifiableList(choiceValues);
        this.maxLength = maxLength;
        this.dateFormat = dateFormat;
    }
}
