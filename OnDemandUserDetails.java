package mx.infotec.imss.infrastructure.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

/**
 * Principal autenticado. Lleva la {@link SessionCredential} sellada, que viaja con
 * el SecurityContext dentro de la HttpSession y muere cuando la sesion expira.
 *
 * getPassword() devuelve null a proposito: el password no se conserva en claro; el
 * material cifrado esta en sessionCredential.
 */
@Getter
public class OnDemandUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final SessionCredential sessionCredential;
    private final Collection<? extends GrantedAuthority> authorities;

    public OnDemandUserDetails(String username,
                               SessionCredential sessionCredential,
                               Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.sessionCredential = sessionCredential;
        this.authorities = authorities;
    }

    @Override
    public String getPassword() {
        return null; // no se conserva en claro
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
