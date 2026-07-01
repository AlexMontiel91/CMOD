package mx.infotec.imss.infrastructure.security;

import lombok.RequiredArgsConstructor;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandCredentials;

/**
 * Adaptador que conecta la credencial sellada de la sesion con el template.
 *
 * Esta es la "costura": el {@link SessionCredential} vive en la sesion (sobre
 * cerrado) y no puede descifrarse solo; este objeto efimero combina ese sobre con
 * el {@link CredentialCipher} (bean) y expone {@link #password()} que descifra al
 * vuelo. Se crea por transaccion y no se guarda en ningun lado.
 */
@RequiredArgsConstructor
public final class SessionBackedCredentials implements OnDemandCredentials {

    private final SessionCredential sealed;
    private final CredentialCipher cipher;

    @Override
    public String getUser() {
        return sealed.getUser();
    }

    @Override
    public char[] password() {
        // devuelve char[] recien descifrado; el template lo limpia tras el logon
        return cipher.open(sealed);
    }
}
