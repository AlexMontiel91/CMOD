package com.app.icncards.infrastructure.odwek.connection;

/**
 * Error tecnico al operar contra OnDemand (red, servidor no disponible, fallo
 * durante busqueda/recuperacion). NO debe filtrar credenciales ni el ODException
 * crudo hacia respuestas HTTP; el mensaje es seguro para log interno.
 *
 * Es RuntimeException para no acoplar las capas de negocio a com.ibm.edms.od.
 * Es una excepcion TECNICA del adaptador, no de dominio.
 */
public class OnDemandException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OnDemandException(String message) {
        super(message);
    }

    public OnDemandException(String message, Throwable cause) {
        super(message, cause);
    }
}
