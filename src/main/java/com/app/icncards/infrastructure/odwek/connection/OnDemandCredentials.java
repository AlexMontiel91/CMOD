package com.app.icncards.infrastructure.odwek.connection;

/**
 * Credencial efimera que el template usa para el logon de UNA transaccion.
 *
 * La capa de seguridad/sesion implementa esta interfaz: normalmente descifrando
 * bajo demanda el SessionCredential sellado (AES-GCM) que vive en la HttpSession.
 * El metodo {@link #password()} debe devolver una COPIA fresca del arreglo en cada
 * llamada, porque el template lo limpia (Arrays.fill) tras el logon. Asi el
 * material en claro existe el minimo tiempo posible.
 */
public interface OnDemandCredentials {

    /** Userid RACF / OnDemand. */
    String getUser();

    /** Copia del password en claro. El template la limpia tras usarla. */
    char[] password();
}
