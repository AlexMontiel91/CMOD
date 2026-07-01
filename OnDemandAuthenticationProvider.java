package mx.infotec.imss.infrastructure.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.infrastructure.odwek.connection.InvalidCredentialsException;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandException;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandOperations;

/**
 * Autentica al usuario contra RACF haciendo un logon REAL en OnDemand. Si el logon
 * pasa, sella la credencial (AES-GCM) y la mete en el principal, que viaja con la
 * sesion. El password en claro se limpia en el finally.
 *
 * NOTA sobre auth-error-ids: mientras ese set (en OnDemandProperties) este vacio,
 * un password incorrecto se clasifica como OnDemandException (tecnico), no como
 * InvalidCredentialsException, y aqui se traduce a AuthenticationServiceException
 * (error de servicio) en vez de "credenciales invalidas". Para que un password
 * malo muestre el mensaje correcto de credenciales, hay que llenar auth-error-ids
 * con los errorId de RACF (del manual Messages and Codes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnDemandAuthenticationProvider implements AuthenticationProvider {

    private final OnDemandOperations onDemand;
    private final CredentialCipher cipher;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String user = authentication.getName();
        char[] pwd = authentication.getCredentials().toString().toCharArray();
        try {
            // Validacion real: logon -> callback trivial -> logoff (dentro del template)
            onDemand.execute(new PlainCredentials(user, pwd), server -> Boolean.TRUE);

            // Logon OK: sellar credencial y construir el principal
            SessionCredential sealed = cipher.seal(user, pwd);
            List<GrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            OnDemandUserDetails principal = new OnDemandUserDetails(user, sealed, authorities);

            log.info("Login OnDemand exitoso para usuario '{}'", user);
            // credenciales en null: el ProviderManager no conserva el password en el token
            return new UsernamePasswordAuthenticationToken(principal, null, authorities);

        } catch (InvalidCredentialsException e) {
            log.info("Login rechazado para usuario '{}': credenciales RACF invalidas", user);
            throw new BadCredentialsException("Credenciales invalidas");

        } catch (OnDemandException e) {
            log.warn("Login no disponible para usuario '{}': fallo tecnico contra OnDemand", user, e);
            throw new AuthenticationServiceException("Servicio OnDemand no disponible", e);

        } finally {
            Arrays.fill(pwd, '\0');
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
