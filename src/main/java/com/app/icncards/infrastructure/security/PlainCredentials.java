package com.app.icncards.infrastructure.security;

import com.app.icncards.infrastructure.odwek.connection.OnDemandCredentials;

/**
 * Credencial en claro de vida muy corta, usada SOLO durante la validacion del
 * login (el AuthenticationProvider hace un logon real con estos datos). Fuera de
 * ese instante no debe existir en memoria.
 */
public final class PlainCredentials implements OnDemandCredentials {

    private final String user;
    private final char[] password;

    public PlainCredentials(String user, char[] password) {
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
