package mx.infotec.imss.infrastructure.odwek.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.ibm.edms.od.ODConfig;
import com.ibm.edms.od.ODServer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.infrastructure.odwek.config.OnDemandProperties;

/**
 * Factory de objetos para el pool de conexiones ODWEK.
 *
 * Modelo "shell": el pool guarda objetos {@link ODServer} ya construidos e
 * inicializados (initialize + setPort + SSL) pero SIN logon. El logon con las
 * credenciales del usuario lo hace el template por transaccion, y el logoff al
 * devolver. Un mismo shell se reutiliza para usuarios distintos.
 *
 * Ciclo de vida:
 *   create        -> new ODServer + initialize (+setPort/+setSSL). Sin logon.
 *   validateObject-> isInitialized(). Si es false, el pool destruye y recrea.
 *   destroyObject -> logoff (defensivo) + terminate. Se invoca al cerrar el pool.
 */
@Slf4j
@RequiredArgsConstructor
public class ODServerPooledObjectFactory extends BasePooledObjectFactory<ODServer> {

    private final OnDemandProperties props;
    private final ODConfig odConfig;

    @Override
    public ODServer create() throws Exception {
        ODServer server = new ODServer(odConfig);
        server.initialize(props.getApplicationName());
        server.setPort(props.getPort());

        OnDemandProperties.Ssl ssl = props.getSsl();
        if (ssl.isEnabled()) {
            // TLS hacia OnDemand (GSKit): keyring .kdb + stash .sth provistos por middleware.
            server.setSSL(true, ssl.getKeyRingFile(), ssl.getKeyStashFile());
        }
        log.debug("Shell ODServer creado e inicializado (sin logon)");
        return server; // <-- sin logon: no hay credenciales aqui
    }

    @Override
    public PooledObject<ODServer> wrap(ODServer server) {
        return new DefaultPooledObject<>(server);
    }

    /**
     * Validacion en borrow (testOnBorrow=true). isInitialized() reporta si el shell
     * fue inicializado y no terminado; NO detecta conexion TCP muerta (de eso se
     * encarga el template, invalidando si el logon falla por error tecnico).
     * Si devuelve false, Commons Pool2 destruye este objeto y crea uno nuevo de
     * forma transparente: no hay que reinicializar en sitio.
     */
    @Override
    public boolean validateObject(PooledObject<ODServer> p) {
        try {
            ODServer s = p.getObject();
            return s != null && s.isInitialized();
        } catch (Exception e) {
            log.warn("validateObject lanzo excepcion; el shell se considera invalido", e);
            return false;
        }
    }

    /**
     * El logoff lo hace el template en su finally (es quien hizo el logon). Aqui no
     * se repite para evitar logoffs dobles ruidosos. Se deja como no-op intencional.
     */
    @Override
    public void passivateObject(PooledObject<ODServer> p) {
        // no-op: el template garantiza el logoff por transaccion
    }

    @Override
    public void destroyObject(PooledObject<ODServer> p) {
        ODServer s = p.getObject();
        if (s == null) {
            return;
        }
        try {
            s.logoff();
        } catch (Exception ignore) {
            // puede no estar logueado; es esperado
        }
        try {
            s.terminate();
            log.debug("Shell ODServer terminado");
        } catch (Exception e) {
            log.warn("Error al terminar ODServer", e);
        }
    }
}
