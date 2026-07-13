package com.app.icncards.infrastructure.security;

/**
 * Sella y abre la credencial del usuario para almacenarla en la HttpSession sin
 * texto claro. Abstraccion (SOLID/DIP): hoy AES-GCM con llave en server.env,
 * manana podria ser un vault sustituyendo solo la implementacion.
 */
public interface CredentialCipher {

    /** Cifra el password y lo empaqueta con el userid en un objeto serializable. */
    SessionCredential seal(String user, char[] password);

    /** Descifra y devuelve una COPIA fresca del password; el llamador debe limpiarla. */
    char[] open(SessionCredential credential);
}
