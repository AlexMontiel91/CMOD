package mx.infotec.imss.infrastructure.odwek.connection;

import java.util.Arrays;

import com.ibm.edms.od.ODException;
import com.ibm.edms.od.ODServer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.infrastructure.odwek.config.OnDemandProperties;
import mx.infotec.imss.infrastructure.odwek.pool.ODServerPool;

/**
 * Implementacion de {@link OnDemandOperations} estilo "template" (como JdbcTemplate):
 * encapsula el patron borrow -> logon -> accion -> logoff -> release/invalidate, de
 * modo que las capas de negocio nunca tocan el pool ni ODServer directamente.
 *
 * Manejo de excepciones (la API de ODWEK declara java.lang.Exception, no solo
 * ODException, por eso hay un catch tipado y otro generico):
 *  - ODException -> se clasifica auth vs tecnico.
 *      * auth (logon rechazado por RACF): el shell esta sano (nunca abrio sesion)
 *        -> se RELEASE; se lanza InvalidCredentialsException para que la capa
 *        superior invalide la sesion y mande a re-login (sin reintentar).
 *      * tecnico: el shell es sospechoso -> se INVALIDATE (se destruye y recrea).
 *  - Exception generica -> error tecnico -> se INVALIDATE.
 *  - RuntimeException de la accion (de dominio o propia) -> se re-lanza tal cual,
 *    sin envolver, e invalidando por precaucion (estado de sesion desconocido).
 */
@Slf4j
@RequiredArgsConstructor
public class OnDemandTemplate implements OnDemandOperations {

    private final ODServerPool pool;
    private final OnDemandProperties props;
    private final ODErrorClassifier classifier;

    @Override
    public <T> T execute(@NonNull OnDemandCredentials credentials, @NonNull ODServerCallback<T> action) {
        ODServer server = null;
        boolean loggedOn = false;
        boolean invalidate = false;
        char[] pwd = credentials.password();
        try {
            server = pool.borrow();

            // --- logon de la transaccion ---
            try {
                server.logon(props.getServerName(), credentials.getUser(), new String(pwd));
                loggedOn = true;
            } catch (ODException e) {
                // shell sano si fue rechazo de credenciales; sospechoso si fue tecnico
                invalidate = !classifier.isAuthFailure(e);
                throw classifier.translateLogon(e);
            } catch (Exception e) {
                invalidate = true;
                throw new OnDemandException("Fallo tecnico en logon OnDemand", e);
            }

            // --- accion de negocio ---
            try {
                return action.doInServer(server);
            } catch (ODException e) {
                invalidate = true; // conservador: ante fallo en sesion, no reusar la conexion
                throw classifier.translate(e);
            } catch (RuntimeException e) {
                invalidate = true; // de dominio o propia: re-lanzar sin envolver
                throw e;
            } catch (Exception e) {
                invalidate = true;
                throw new OnDemandException("Error en operacion OnDemand", e);
            }

        } finally {
            if (pwd != null) {
                Arrays.fill(pwd, '\0'); // higiene: limpia el password en claro
            }
            cleanup(server, loggedOn, invalidate);
        }
    }

    /** Cierra la sesion del usuario y devuelve o invalida la conexion en el pool. */
    private void cleanup(ODServer server, boolean loggedOn, boolean invalidate) {
        if (server == null) {
            return;
        }
        if (loggedOn) {
            try {
                server.logoff();
            } catch (Exception e) {
                invalidate = true; // si no se pudo cerrar sesion limpia, no reusar
                log.warn("Fallo en logoff; se invalidara la conexion", e);
            }
        }
        if (invalidate) {
            pool.invalidate(server);
        } else {
            pool.release(server);
        }
    }
}
