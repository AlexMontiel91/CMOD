package com.app.icncards.infrastructure.odwek.connection;

/**
 * La contrasena es correcta pero VENCIO (RACF/OnDemand exige cambiarla). NO es lo
 * mismo que credenciales invalidas: la conexion es sana (se DEVUELVE al pool) y,
 * a diferencia de InvalidCredentialsException, esto NUNCA debe contar para el
 * bloqueo de intentos (LoginAttemptService) -- el usuario no se equivoco, solo
 * necesita actualizar su password.
 *
 * Puede ocurrir tanto en el login inicial como a media sesion (si la contrasena
 * vence entre que el usuario entra y una transaccion posterior). Ambos casos se
 * manejan: el login en LoginController/OnDemandAuthenticationProvider, el de
 * media sesion en GlobalExceptionHandler.
 */
public class PasswordExpiredException extends OnDemandException {

    private static final long serialVersionUID = 1L;

    public PasswordExpiredException(String message) {
        super(message);
    }

    public PasswordExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
