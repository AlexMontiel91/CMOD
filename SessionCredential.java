package mx.infotec.imss.infrastructure.security;

import java.io.Serializable;

import lombok.Getter;

/**
 * Credencial sellada que vive en la HttpSession. Contiene el userid en claro (no es
 * secreto) y el password cifrado con AES-GCM (iv + cipherText). Nunca password en
 * claro. Es Serializable porque puede persistirse/replicarse con la sesion.
 */
@Getter
public final class SessionCredential implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String user;
    private final byte[] iv;
    private final byte[] cipherText;

    public SessionCredential(String user, byte[] iv, byte[] cipherText) {
        this.user = user;
        this.iv = iv;
        this.cipherText = cipherText;
    }
}
