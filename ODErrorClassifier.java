package mx.infotec.imss.infrastructure.odwek.connection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.edms.od.ODException;

/**
 * Traduce un {@link ODException} de ODWEK a la jerarquia de excepciones del
 * adaptador, distinguiendo fallo de autenticacion (no reintentar) de error tecnico.
 *
 * La clasificacion se basa en el errorId de OnDemand. Los IDs de autenticacion
 * son configurables (ondemand.auth-error-ids) porque dependen de la version del
 * servidor y se obtienen del manual "Messages and Codes". Mientras el set este
 * vacio, todo se clasifica como tecnico (comportamiento seguro: no se invalida
 * la sesion del usuario por error).
 *
 * Codigos conocidos por la guia de implementacion (referencia):
 *   2086 = "Connection cannot be established" (host/server invalido) -> TECNICO
 *   2170 = "No hits specified" (hits obsoletos)                      -> TECNICO/uso
 */
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
        if (isAuthFailure(e)) {
            return new InvalidCredentialsException(message("Logon rechazado", e), e);
        }
        return new OnDemandException(message("Fallo tecnico en logon OnDemand", e), e);
    }

    /** Traduce un fallo ocurrido durante la transaccion (busqueda/recuperacion). */
    public OnDemandException translate(ODException e) {
        if (isAuthFailure(e)) {
            return new InvalidCredentialsException(message("Credencial rechazada", e), e);
        }
        return new OnDemandException(message("Error en operacion OnDemand", e), e);
    }

    private String message(String prefix, ODException e) {
        String detail;
        try {
            detail = e.getErrorMsg();
        } catch (Exception ignore) {
            detail = "(sin mensaje)";
        }
        return prefix + " [id=" + e.getErrorId() + "]: " + detail;
    }
}
