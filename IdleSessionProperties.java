package mx.infotec.imss.infrastructure.security;

import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Minutos de inactividad tras los cuales el temporizador del NAVEGADOR cierra la
 * sesion proactivamente (protege pantallas desatendidas en equipos de sucursal).
 *
 * IMPORTANTE: esto es UX, no el control de seguridad real. La seguridad real es
 * el invalidationTimeout de <httpSession> en server.xml de Liberty, que mata la
 * sesion en el SERVIDOR sin depender del navegador. Mantener este valor igual o
 * menor al de server.xml (coordinar con el equipo de middleware al pedir el
 * bloque <httpSession> custom).
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security.idle")
public class IdleSessionProperties {

    @Min(1)
    private int timeoutMinutes = 15;
}
