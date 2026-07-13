package com.app.icncards.infrastructure.odwek.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuracion de la conexion a Content Manager OnDemand (ODWEK) y del pool de
 * conexiones {@code ODServer}. Se enlaza por relaxed-binding: las variables de
 * entorno (p. ej. inyectadas por Liberty desde server.env) mapean automaticamente
 * (ONDEMAND_SERVER_NAME -> ondemand.server-name, etc.).
 *
 * Host y puerto son constantes para una unica instancia OnDemand; las credenciales
 * NO viven aqui, se pasan por transaccion en el template.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ondemand")
public class OnDemandProperties {

    /** Nombre o IP del library server OnDemand (host del mainframe). */
    @NotBlank
    private String serverName;

    /** Puerto de escucha del servidor OnDemand. */
    @Min(1)
    @Max(65535)
    private int port = 1450;

    /** Nombre de aplicacion que se reporta en initialize() y en el System Log de OnDemand. */
    @NotBlank
    private String applicationName = "backend-imss";

    /** Tope global de hits por busqueda (control de memoria). Default OnDemand = 200. */
    @Min(1)
    @Max(5000)
    private int maxHits = 200;

    /** Codigo de idioma de ODWEK (ENU, ESP, etc.). */
    @NotBlank
    private String language = "ENU";

    /** Directorio temporal de ODWEK. Si es null se usa java.io.tmpdir. */
    private String tempDir = System.getProperty("java.io.tmpdir");

    /** Habilita el trace binario de ODWEK. SIEMPRE false en produccion. */
    private boolean traceEnabled = false;

    /** Nivel de trace 0..4 (solo aplica si traceEnabled=true; lo indica IBM Support). */
    @Min(0)
    @Max(4)
    private int traceLevel = 0;

    /** Directorio del trace. Si es null se usa tempDir. */
    private String traceDir;

    /**
     * IDs de error de ODException que representan fallo de autenticacion
     * (password incorrecto, userid revocado, password expirado). Se obtienen del
     * manual "Messages and Codes". Mientras este set este vacio, cualquier fallo
     * de logon se trata como tecnico (no se fuerza re-login). LLENAR antes de prod.
     */
    private Set<Integer> authErrorIds = new HashSet<>();

    /**
     * IDs de error que significan "password vencido, requiere cambio" (distinto de
     * credenciales invalidas: NO debe contar para el bloqueo de intentos). 2061
     * confirmado empiricamente contra un servidor real ("Cambiar contrasena de
     * conexion caduca"); coincide con el escenario documentado de
     * ODConstant.OD_ARCMSG_CLIENT_CHANGE_EXPIRED_PASSWORD.
     */
    private Set<Integer> passwordExpiredErrorIds = new HashSet<>(Collections.singletonList(2061));

    @NestedConfigurationProperty
    private Ssl ssl = new Ssl();

    @NestedConfigurationProperty
    private Pool pool = new Pool();

    /** Cifrado TLS del tramo hacia OnDemand (GSKit: keyring .kdb + stash .sth). */
    @Getter
    @Setter
    public static class Ssl {
        private boolean enabled = false;
        private String keyRingFile;
        private String keyStashFile;
    }

    /** Parametros del GenericObjectPool de shells ODServer. */
    @Getter
    @Setter
    public static class Pool {
        /** Maximo de conexiones (= maximo de transacciones OnDemand concurrentes). */
        @Min(1)
        private int maxTotal = 20;
        private int maxIdle = 20;
        private int minIdle = 5;
        /** Espera maxima por una conexion antes de fallar (ms). Evita agotar hilos. */
        private long maxWaitMillis = 10_000L;
        private boolean testOnBorrow = true;
        private boolean testWhileIdle = true;
        private long timeBetweenEvictionRunsMillis = 60_000L;
        private long minEvictableIdleTimeMillis = 300_000L;
        /** Precrea minIdle shells al arrancar la aplicacion. */
        private boolean warmUpOnStartup = true;
    }
}
