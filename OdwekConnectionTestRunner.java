package mx.infotec.imss.infrastructure.odwek.devtest;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.ibm.edms.od.ODServer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.infrastructure.odwek.connection.InvalidCredentialsException;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandCredentials;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandException;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandOperations;

/**
 * *** CLASE DESECHABLE — SOLO PARA PROBAR CONECTIVIDAD EN DEV. BORRAR ANTES DE MERGEAR. ***
 *
 * Dispara un logon REAL contra OnDemand para validar host/puerto/SSL/RACF de punta
 * a punta, sin esperar al incremento de sesiones. Se activa SOLO con el perfil
 * "odwek-conntest", asi que nunca corre por accidente en otro ambiente.
 *
 * Credenciales por variable de entorno (NO hardcodeadas, NO en el repo):
 *   ODWEK_TEST_USER, ODWEK_TEST_PASSWORD
 *
 * Ejecutar (ejemplo):
 *   ODWEK_TEST_USER=miUsuario ODWEK_TEST_PASSWORD=miPass \
 *   java -jar target/NOMBRE.jar --spring.profiles.active=odwek-conntest
 */
@Slf4j
@Component
@Profile("odwek-conntest")
@RequiredArgsConstructor
public class OdwekConnectionTestRunner implements CommandLineRunner {

    private final OnDemandOperations onDemand;
    private final Environment env;

    @Override
    public void run(String... args) {
        String user = env.getProperty("ODWEK_TEST_USER");
        String password = env.getProperty("ODWEK_TEST_PASSWORD");

        if (user == null || password == null) {
            log.error("[CONNTEST] Falta ODWEK_TEST_USER y/o ODWEK_TEST_PASSWORD. Prueba abortada.");
            return;
        }

        log.info("[CONNTEST] Iniciando logon de prueba contra OnDemand con usuario '{}'...", user);

        OnDemandCredentials creds = new TestCredentials(user, password.toCharArray());

        try {
            String serverName = onDemand.execute(creds, (ODServer server) -> {
                // Accion minima: si llegamos aqui, el logon REAL ya tuvo exito.
                return server.getServerName();
            });
            log.info("[CONNTEST] OK - Conexion y logon exitosos. Servidor OnDemand: {}", serverName);

        } catch (InvalidCredentialsException e) {
            log.error("[CONNTEST] FALLO DE AUTENTICACION - el logon llego al mainframe pero RACF "
                    + "rechazo las credenciales (o el set auth-error-ids ya las clasifica). Detalle: {}",
                    e.getMessage());

        } catch (OnDemandException e) {
            log.error("[CONNTEST] FALLO TECNICO - no se pudo establecer/operar la conexion "
                    + "(host/puerto/SSL/red). Detalle: {}", e.getMessage(), e);

        } catch (Exception e) {
            log.error("[CONNTEST] ERROR INESPERADO en la prueba de conexion", e);
        }
    }

    /** Credencial efimera solo para la prueba. */
    private static final class TestCredentials implements OnDemandCredentials {
        private final String user;
        private final char[] password;

        TestCredentials(String user, char[] password) {
            this.user = user;
            this.password = password;
        }

        @Override
        public String getUser() {
            return user;
        }

        @Override
        public char[] password() {
            // copia: el template limpia el arreglo que recibe tras el logon
            return password.clone();
        }
    }
}
