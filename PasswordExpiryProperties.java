package mx.infotec.imss.infrastructure.security;

import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Umbral (dias) para mostrar el aviso de "su contrasena esta por vencer" en el
 * home tras el login. El dato real (dias restantes) viene de
 * ODUser.getNumDaysUntilPWExp(); este umbral solo decide cuando mostrarlo.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security.password-expiry")
public class PasswordExpiryProperties {

    @Min(1)
    private int warnDays = 14;
}
