package mx.infotec.imss.infrastructure.odwek.adapter;

import com.ibm.edms.od.ODConstant;
import com.ibm.edms.od.ODCriteria;

import mx.infotec.imss.domain.model.SearchFieldKind;

/**
 * Traduce ODCriteria.getType() + getSubType() a nuestro {@link SearchFieldKind}.
 *
 * Correccion clave: getType() (char) describe el MECANISMO de entrada (Normal,
 * Choice, Segment, TextSearch...), NO el tipo de dato. Para saber si un campo es
 * fecha hay que usar getSubType() (OD_FLD_DATE / OD_FLD_DATETIME), un metodo
 * distinto. Confundir ambos es el error mas comun al mapear folders de ODWEK.
 */
final class ODFieldKindMapper {

    private ODFieldKindMapper() {
    }

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
        if (isNumeric(subType)) {
            return SearchFieldKind.NUMBER;
        }
        return SearchFieldKind.TEXT;
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
