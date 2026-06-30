package mx.infotec.imss.infrastructure.odwek.connection;

import java.util.Arrays;

import com.ibm.edms.od.ODException;
import com.ibm.edms.od.ODServer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.infrastructure.odwek.config.OnDemandProperties;
import mx.infotec.imss.infrastructure.odwek.pool.ODServerPool;

/**
 * Implementacion de {@link OnDemandOperations} estilo "template" (como JdbcTemplate):
 * encapsula el patron borrow -> logon -> accion -> logoff -> release/invalidate, de
 * modo que las capas de negocio nunca tocan el pool ni ODServer directamente.
 *
 * Decision clave (no revocar usuarios RACF): se distingue fallo de autenticacion
 * de error tecnico.
 *  - Logon rechazado (auth): el shell esta sano (nunca abrio sesion) -> se RELEASE
 *    al pool; se lanza InvalidCredentialsException para que la capa superior
 *    invalide la sesion y mande a re-login (sin reintentar).
 *  - Error tecnico (red/servidor) o fallo durante la transaccion: el shell es
 *    sospechoso -> se INVALIDATE (el pool lo destruye y recrea).
 */
@Slf4j
@RequiredArgsConstructor
public class OnDemandTemplate implements OnDemandOperations {

    private final ODServerPool pool;
    private final OnDemandProperties props;
    private final ODErrorClassifier classifier;

    @Override
    public <T> T execute(OnDemandCredentials credentials, ODServerCallback<T> action) {
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
                OnDemandException mapped = classifier.translateLogon(e);
                // shell sano si fue rechazo de credenciales; sospechoso si fue tecnico
                invalidate = !classifier.isAuthFailure(e);
                throw mapped;
            }

            // --- accion de negocio ---
            try {
                return action.doInServer(server);
            } catch (ODException e) {
                invalidate = true; // conservador: ante fallo en sesion, no reusar la conexion
                throw classifier.translate(e);
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
