package mx.infotec.imss.domain.model;

/**
 * Como debe renderizarse el campo en el formulario. Se deriva en el adaptador
 * combinando ODCriteria.getType() (mecanismo de entrada) y getSubType() (tipo de
 * dato real); el dominio solo ve el resultado, ya resuelto.
 */
public enum SearchFieldKind {
    TEXT,
    NUMBER,
    DATE,
    DATETIME,
    CHOICE
}
