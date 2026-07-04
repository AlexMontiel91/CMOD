package mx.infotec.imss.infrastructure.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import mx.infotec.imss.domain.model.UserLoginInfo;

/**
 * Principal autenticado. Lleva la {@link SessionCredential} sellada, que viaja con
 * el SecurityContext dentro de la HttpSession y muere cuando la sesion expira.
 * Tambien lleva {@link UserLoginInfo} (ultimo logon, intentos fallidos, dias para
 * expirar password), capturado una sola vez durante el login para no repetir la
 * consulta a OnDemand en cada pantalla.
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
    private final UserLoginInfo loginInfo;

    public OnDemandUserDetails(String username,
                               SessionCredential sessionCredential,
                               Collection<? extends GrantedAuthority> authorities,
                               UserLoginInfo loginInfo) {
        this.username = username;
        this.sessionCredential = sessionCredential;
        this.authorities = authorities;
        this.loginInfo = loginInfo;
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
