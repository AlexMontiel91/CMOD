package com.app.icncards.infrastructure.odwek.connection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.edms.od.ODException;

import lombok.extern.slf4j.Slf4j;

/**
 * Traduce un {@link ODException} de ODWEK a la jerarquia de excepciones del
 * adaptador, distinguiendo TRES categorias (en este orden de prioridad):
 *
 *  1. Password vencido -> PasswordExpiredException. NO cuenta para el bloqueo de
 *     intentos: el usuario no se equivoco, solo necesita cambiar su password.
 *  2. Fallo de autenticacion (credenciales invalidas) -> InvalidCredentialsException.
 *     La conexion sigue sana (nunca abrio sesion) -> se devuelve al pool.
 *  3. Tecnico -> OnDemandException. El shell es sospechoso -> se invalida.
 *
 * Los IDs de cada categoria son configurables (ondemand.auth-error-ids,
 * ondemand.password-expired-error-ids) porque dependen de la version del
 * servidor y se obtienen del manual "Messages and Codes" o empiricamente
 * (capturando el errorId real en el log [ODWEK errorId] de este clasificador).
 *
 * Codigos conocidos:
 *   2061 = password vencido, requiere cambio (confirmado empiricamente; coincide
 *          con el escenario documentado ODConstant.OD_ARCMSG_CLIENT_CHANGE_EXPIRED_PASSWORD)
 *   2107 = ARS2107E "El ID de usuario o password no es valido" (confirmado: doc
 *          oficial + capturado en log real)
 *   2086 = "Connection cannot be established" (host/server invalido) -> TECNICO
 *   2170 = "No hits specified" (hits obsoletos)                      -> TECNICO/uso
 */
@Slf4j
public class ODErrorClassifier {

    private final Set<Integer> authErrorIds;
    private final Set<Integer> passwordExpiredErrorIds;

    public ODErrorClassifier(Set<Integer> authErrorIds, Set<Integer> passwordExpiredErrorIds) {
        this.authErrorIds = toSet(authErrorIds);
        this.passwordExpiredErrorIds = toSet(passwordExpiredErrorIds);
    }

    private static Set<Integer> toSet(Set<Integer> ids) {
        return ids == null ? Collections.<Integer>emptySet() : new HashSet<>(ids);
    }

    public boolean isAuthFailure(ODException e) {
        return authErrorIds.contains((int) e.getErrorId());
    }

    public boolean isPasswordExpired(ODException e) {
        return passwordExpiredErrorIds.contains((int) e.getErrorId());
    }

    /** Traduce un fallo ocurrido en el logon (login inicial o por transaccion). */
    public OnDemandException translateLogon(ODException e) {
        if (isPasswordExpired(e)) {
            logError("logon", e, "PASSWORD_EXPIRED");
            return new PasswordExpiredException(message("Password vencido", e), e);
        }
        if (isAuthFailure(e)) {
            logError("logon", e, "AUTH");
            return new InvalidCredentialsException(message("Logon rechazado", e), e);
        }
        logError("logon", e, "TECNICO");
        return new OnDemandException(message("Fallo tecnico en logon OnDemand", e), e);
    }

    /** Traduce un fallo ocurrido durante la transaccion (busqueda/recuperacion/cambio de password). */
    public OnDemandException translate(ODException e) {
        if (isPasswordExpired(e)) {
            logError("transaccion", e, "PASSWORD_EXPIRED");
            return new PasswordExpiredException(message("Password vencido", e), e);
        }
        if (isAuthFailure(e)) {
            logError("transaccion", e, "AUTH");
            return new InvalidCredentialsException(message("Credencial rechazada", e), e);
        }
        logError("transaccion", e, "TECNICO");
        return new OnDemandException(message("Error en operacion OnDemand", e), e);
    }

    /**
     * Log destacado para identificar el errorId. Busca "[ODWEK errorId]" en el log.
     * Cuando sepas que numero corresponde a un caso nuevo, agregalo al set
     * correspondiente en application.yml y quedara clasificado automaticamente.
     */
    private void logError(String fase, ODException e, String clasificacion) {
        log.warn("[ODWEK errorId] fase={} id={} clasificado={} msg={}",
                fase, e.getErrorId(), clasificacion, safeMsg(e));
    }

    private String message(String prefix, ODException e) {
        return prefix + " [id=" + e.getErrorId() + "]: " + safeMsg(e);
    }

    private String safeMsg(ODException e) {
        try {
            return e.getErrorMsg();
        } catch (Exception ignore) {
            return "(sin mensaje)";
        }
    }
}
