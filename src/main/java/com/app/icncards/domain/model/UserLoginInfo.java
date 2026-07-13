package com.app.icncards.domain.model;

import java.io.Serializable;
import java.time.Instant;

import lombok.Getter;

/**
 * Informacion complementaria de la sesion, obtenida de ODUser durante el logon.
 * Cualquier campo puede venir null/0 si ODWEK no lo pudo entregar (nunca debe
 * bloquear el login por fallar en obtener esto). Sin dependencias de ODWEK.
 */
@Getter
public class UserLoginInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Fecha del ultimo logon EXITOSO anterior. Ver nota de verificacion en
     *  OnDemandAuthenticationProvider sobre si refleja el login anterior o el actual. */
    private final Instant lastLogonAt;

    /** Intentos fallidos reportados por el propio servidor (mas autoritativo que
     *  el contador local de LoginAttemptService, que es solo defensivo). */
    private final int failedLoginsSinceLastSuccess;

    /** Dias para que expire la contrasena. Null si no se pudo determinar. */
    private final Integer daysUntilPasswordExpires;

    public UserLoginInfo(Instant lastLogonAt, int failedLoginsSinceLastSuccess,
                         Integer daysUntilPasswordExpires) {
        this.lastLogonAt = lastLogonAt;
        this.failedLoginsSinceLastSuccess = failedLoginsSinceLastSuccess;
        this.daysUntilPasswordExpires = daysUntilPasswordExpires;
    }

    /** Sin datos disponibles (fallback seguro si ODUser no pudo consultarse). */
    public static UserLoginInfo unavailable() {
        return new UserLoginInfo(null, 0, null);
    }
}
