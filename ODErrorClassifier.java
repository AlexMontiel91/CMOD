package mx.infotec.imss.infrastructure.odwek.connection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.edms.od.ODException;

import lombok.extern.slf4j.Slf4j;

/**
 * Traduce un {@link ODException} de ODWEK a la jerarquia de excepciones del
 * adaptador, distinguiendo fallo de autenticacion (no reintentar) de error tecnico.
 *
 * La clasificacion se basa en el errorId de OnDemand. Los IDs de autenticacion
 * son configurables (ondemand.auth-error-ids) porque dependen de la version del
 * servidor y se obtienen del manual "Messages and Codes". Mientras el set este
 * vacio, todo se clasifica como tecnico (comportamiento seguro).
 *
 * DESCUBRIMIENTO DE IDs: cada ODException se registra en el log con una etiqueta
 * "[ODWEK errorId]" para poder identificar que numero corresponde a "password
 * incorrecto", "userid revocado", etc., y asi llenar ondemand.auth-error-ids.
 * Provoca un login con password malo y busca esa etiqueta en el log.
 *
 * Codigos conocidos por la guia de implementacion (referencia):
 *   2086 = "Connection cannot be established" (host/server invalido) -> TECNICO
 *   2170 = "No hits specified" (hits obsoletos)                      -> TECNICO/uso
 */
@Slf4j
public class ODErrorClassifier {

    private final Set<Integer> authErrorIds;

    public ODErrorClassifier(Set<Integer> authErrorIds) {
        this.authErrorIds = authErrorIds == null
                ? Collections.<Integer>emptySet()
                : new HashSet<>(authErrorIds);
    }

    public boolean isAuthFailure(ODException e) {
        // Cast defensivo: el errorId es numerico (verificar tipo exacto en el Javadoc
        // de ODException). El cast a int funciona tanto si la API devuelve int como long.
        return authErrorIds.contains((int) e.getErrorId());
    }

    /** Traduce un fallo ocurrido en el logon. */
    public OnDemandException translateLogon(ODException e) {
        boolean auth = isAuthFailure(e);
        logError("logon", e, auth);
        if (auth) {
            return new InvalidCredentialsException(message("Logon rechazado", e), e);
        }
        return new OnDemandException(message("Fallo tecnico en logon OnDemand", e), e);
    }

    /** Traduce un fallo ocurrido durante la transaccion (busqueda/recuperacion). */
    public OnDemandException translate(ODException e) {
        boolean auth = isAuthFailure(e);
        logError("transaccion", e, auth);
        if (auth) {
            return new InvalidCredentialsException(message("Credencial rechazada", e), e);
        }
        return new OnDemandException(message("Error en operacion OnDemand", e), e);
    }

    /**
     * Log destacado para identificar el errorId. Busca "[ODWEK errorId]" en el log.
     * Cuando sepas que numero es "password incorrecto", agregalo a
     * ondemand.auth-error-ids y quedara clasificado como AUTH automaticamente.
     */
    private void logError(String fase, ODException e, boolean auth) {
        log.warn("[ODWEK errorId] fase={} id={} clasificado={} msg={}",
                fase, e.getErrorId(), auth ? "AUTH" : "TECNICO", safeMsg(e));
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
