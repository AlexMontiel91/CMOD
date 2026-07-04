package mx.infotec.imss.domain.model;

/**
 * Operadores de busqueda soportados. Enum = llave ESTABLE para el <select> del
 * formulario y para el mapeo a los codigos de ODWEK (ODConstant.OPxxx), separada
 * de la etiqueta visible al usuario (que vive en messages.properties, no aqui).
 * Sin esta separacion, localizar la etiqueta rompe el mapeo reverso.
 */
public enum SearchOperator {
    EQUAL,
    NOT_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    IN,
    NOT_IN,
    LIKE,
    NOT_LIKE,
    BETWEEN,
    NOT_BETWEEN;

    /** true si este operador requiere DOS valores (p. ej. rango de fechas). */
    public boolean requiresTwoValues() {
        return this == BETWEEN || this == NOT_BETWEEN;
    }
}
