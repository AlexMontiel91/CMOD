package mx.infotec.imss.infrastructure.security;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Llave AES para cifrar la credencial en sesion. El valor (Base64 de 32 bytes =
 * AES-256) vive en server.env de Liberty y se referencia por variable de entorno:
 *
 *   server.env:              APP_SECURITY_CIPHER_KEY=xxxxBase64xxxx
 *   application.yml:         app.security.cipher-key: ${APP_SECURITY_CIPHER_KEY}
 *
 * Proteccion real: permisos del archivo server.env (chmod 600), no en el repo.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security")
public class CipherProperties {

    /** Llave AES-256 en Base64 (32 bytes decodificados). */
    @NotBlank
    private String cipherKey;
}
