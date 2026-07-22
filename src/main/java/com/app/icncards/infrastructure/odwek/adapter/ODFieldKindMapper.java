package com.app.icncards.infrastructure.odwek.adapter;

import com.ibm.edms.od.ODConstant;
import com.ibm.edms.od.ODCriteria;

import com.app.icncards.domain.model.SearchFieldKind;

/**
 * Traduce ODCriteria.getType() + getSubType() a nuestro {@link SearchFieldKind}.
 *
 * Correccion clave: getType() (char) describe el MECANISMO de entrada (Normal,
 * Choice, Segment, TextSearch...), NO el tipo de dato. Para saber si un campo es
 * fecha hay que usar getSubType() (OD_FLD_DATE / OD_FLD_DATETIME), un metodo
 * distinto. Confundir ambos es el error mas comun al mapear folders de ODWEK.
 *
 * Fallback por formato: algunos folders de CMOD definen fechas con subtipo
 * NUMERICO y expresan la fecha en el FORMATO (getDefaultFmt, estilo strftime
 * "%m/%d/%y"), no en getSubType(). Sin este fallback esas fechas se clasificarian
 * como NUMBER y se renderizarian como campo numerico en vez de selector de fecha.
 */
final class ODFieldKindMapper {

    private ODFieldKindMapper() {
    }

    // Especificadores strftime (case-sensitive: %m=mes vs %M=minuto, %H=hora, %S=segundo).
    private static final String[] DATE_SPECS =
            {"%y", "%Y", "%m", "%d", "%e", "%j", "%b", "%B", "%a", "%A", "%D", "%F"};
    private static final String[] TIME_SPECS =
            {"%H", "%I", "%M", "%S", "%p", "%r", "%R", "%T", "%X"};

    static SearchFieldKind resolve(ODCriteria criteria) {
        char type = criteria.getType();
        if (type == ODConstant.InputTypeChoice || type == ODConstant.InputTypeSegment) {
            return SearchFieldKind.CHOICE;
        }

        char subType = criteria.getSubType();
        if (subType == ODConstant.OD_FLD_DATETIME) {
            return SearchFieldKind.DATETIME;
        }
        if (subType == ODConstant.OD_FLD_DATE) {
            return SearchFieldKind.DATE;
        }

        // Antes de tratarlo como numero/texto, ver si el FORMATO revela una fecha
        // (campos CMOD con subtipo numerico pero formato de fecha).
        SearchFieldKind byFormat = resolveByFormat(criteria);
        if (byFormat != null) {
            return byFormat;
        }

        if (isNumeric(subType)) {
            return SearchFieldKind.NUMBER;
        }
        return SearchFieldKind.TEXT;
    }

    /**
     * Deduce fecha/fecha-hora a partir del formato de ODWEK (getDefaultFmt). Devuelve
     * null si el formato no parece de fecha (para que el campo siga su clasificacion
     * por subtipo). Un formato con hora ademas de fecha se trata como DATETIME.
     */
    private static SearchFieldKind resolveByFormat(ODCriteria criteria) {
        String fmt = criteria.getDefaultFmt();
        if (fmt == null || fmt.isEmpty() || !containsAny(fmt, DATE_SPECS)) {
            return null;
        }
        return containsAny(fmt, TIME_SPECS) ? SearchFieldKind.DATETIME : SearchFieldKind.DATE;
    }

    /** contains case-sensitive (no se puede bajar a minusculas: %m/%M significan cosas distintas). */
    private static boolean containsAny(String haystack, String[] needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNumeric(char subType) {
        return subType == ODConstant.OD_FLD_INTEGER
                || subType == ODConstant.OD_FLD_SMALLINT
                || subType == ODConstant.OD_FLD_BIGINT
                || subType == ODConstant.OD_FLD_DECIMAL
                || subType == ODConstant.OD_FLD_DECFLOAT16
                || subType == ODConstant.OD_FLD_DECFLOAT34;
    }
}
