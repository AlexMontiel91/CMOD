package com.app.icncards.infrastructure.odwek.adapter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.ibm.edms.od.ODConstant;

import com.app.icncards.domain.model.SearchOperator;

/**
 * Traduce entre {@link SearchOperator} (dominio, llave estable) y los codigos int
 * de ODConstant.OPxxx que espera ODCriteria.setOperator(int)/getOperator().
 *
 * Deliberadamente NO se usa el texto de la etiqueta visible como llave (a
 * diferencia de un mapeo por nombre en ingles/espanol): si se localiza la
 * etiqueta, el mapeo no debe romperse. La llave es siempre el enum.
 */
final class ODOperatorCodec {

    private static final Map<SearchOperator, Integer> TO_CODE = new EnumMap<>(SearchOperator.class);
    private static final Map<Integer, SearchOperator> FROM_CODE = new HashMap<>();

    static {
        map(SearchOperator.EQUAL, ODConstant.OPEqual);
        map(SearchOperator.NOT_EQUAL, ODConstant.OPNotEqual);
        map(SearchOperator.LESS_THAN, ODConstant.OPLessThan);
        map(SearchOperator.LESS_THAN_OR_EQUAL, ODConstant.OPLessThanEqual);
        map(SearchOperator.GREATER_THAN, ODConstant.OPGreaterThan);
        map(SearchOperator.GREATER_THAN_OR_EQUAL, ODConstant.OPGreaterThanEqual);
        map(SearchOperator.IN, ODConstant.OPIn);
        map(SearchOperator.NOT_IN, ODConstant.OPNotIn);
        map(SearchOperator.LIKE, ODConstant.OPLike);
        map(SearchOperator.NOT_LIKE, ODConstant.OPNotLike);
        map(SearchOperator.BETWEEN, ODConstant.OPBetween);
        map(SearchOperator.NOT_BETWEEN, ODConstant.OPNotBetween);
    }

    private ODOperatorCodec() {
    }

    private static void map(SearchOperator operator, int code) {
        TO_CODE.put(operator, code);
        FROM_CODE.put(code, operator);
    }

    /** null si el folder trae un operador que esta UI no soporta todavia. */
    static SearchOperator fromCode(int code) {
        return FROM_CODE.get(code);
    }

    /** Para el siguiente incremento (ejecutar la busqueda): dominio -> codigo ODWEK. */
    static int toCode(SearchOperator operator) {
        Integer code = TO_CODE.get(operator);
        if (code == null) {
            throw new IllegalArgumentException("Operador sin mapeo a ODWEK: " + operator);
        }
        return code;
    }
}
