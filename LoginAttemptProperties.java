package mx.infotec.imss.infrastructure.security;

import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Parametros del bloqueo de intentos de login (anti fuerza bruta / proteccion del
 * userid RACF). El default es 2 porque RACF revoca a los 3: cortamos antes.
 *
 * IMPORTANTE: confirmar con el equipo RACF el valor real de REVOKE(n) y ajustar
 * max-attempts a REVOKE-1.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security.login")
public class LoginAttemptProperties {

    /** Fallos permitidos antes de bloquear. Debe ser REVOKE(n) de RACF menos 1. */
    @Min(1)
    private int maxAttempts = 2;

    /** Minutos de enfriamiento del bloqueo (anti-martilleo; NO reinicia el contador RACF). */
    @Min(1)
    private long lockMinutes = 15;
}
