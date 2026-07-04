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

import com.ibm.edms.od.ODServer;
import com.ibm.edms.od.ODUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.domain.model.UserLoginInfo;
import mx.infotec.imss.infrastructure.odwek.connection.InvalidCredentialsException;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandException;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandOperations;

/**
 * Autentica al usuario contra RACF haciendo un logon REAL en OnDemand. Si el logon
 * pasa, sella la credencial (AES-GCM) y captura informacion complementaria de
 * ODUser (ultimo logon, intentos fallidos, dias para expirar password) en el
 * MISMO logon, sin una llamada extra al mainframe.
 *
 * NOTA A VERIFICAR: ODUser.getLastLogonDateObj() se describe como "Last
 * Successful Logon Date". No esta confirmado si, consultado justo despues de
 * ESTE logon, ya refleja el logon actual o todavia el anterior (el dato util
 * para mostrar es el anterior). Validar con dos logons reales consecutivos: si
 * el segundo login muestra la hora del primero, el comportamiento es el
 * esperado; si muestra su propia hora, este dato no sirve para "ultima conexion"
 * tal cual y habria que buscar otra fuente.
 *
 * Todo lo de ODUser es "best effort": un fallo aqui NUNCA debe tumbar un login
 * que de otro modo fue exitoso (por eso fetchLoginInfo no declara throws y
 * atrapa cualquier excepcion internamente).
 *
 * NOTA sobre auth-error-ids: mientras ese set (en OnDemandProperties) este vacio,
 * un password incorrecto se clasifica como OnDemandException (tecnico), no como
 * InvalidCredentialsException, y aqui se traduce a AuthenticationServiceException
 * (error de servicio) en vez de "credenciales invalidas".
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
            // Validacion real: logon -> captura info de ODUser -> logoff (en el template)
            UserLoginInfo loginInfo = onDemand.execute(new PlainCredentials(user, pwd), this::fetchLoginInfo);

            // Logon OK: sellar credencial y construir el principal
            SessionCredential sealed = cipher.seal(user, pwd);
            List<GrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            OnDemandUserDetails principal = new OnDemandUserDetails(user, sealed, authorities, loginInfo);

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

    /**
     * Best effort: obtiene datos complementarios de ODUser. NUNCA lanza excepcion
     * (un fallo aqui no debe impedir un login que ya fue validado exitosamente);
     * cualquier campo que no se pueda obtener queda en null/0.
     */
    private UserLoginInfo fetchLoginInfo(ODServer server) {
        java.time.Instant lastLogonAt = null;
        int failedLogins = 0;
        Integer daysUntilPasswordExpires = null;

        try {
            ODUser odUser = server.getUser();

            try {
                java.util.Date lastLogon = odUser.getLastLogonDateObj();
                if (lastLogon != null) {
                    lastLogonAt = lastLogon.toInstant();
                }
            } catch (Exception e) {
                log.debug("No se pudo obtener la fecha del ultimo logon (no bloqueante)", e);
            }

            try {
                failedLogins = odUser.getNumFailedLogins();
            } catch (Exception e) {
                log.debug("No se pudo obtener el numero de logins fallidos (no bloqueante)", e);
            }

            try {
                daysUntilPasswordExpires = odUser.getNumDaysUntilPWExp();
            } catch (Exception e) {
                log.debug("No se pudo obtener los dias para expiracion de password (no bloqueante)", e);
            }

        } catch (Exception e) {
            log.debug("No se pudo obtener el objeto ODUser (no bloqueante)", e);
        }

        return new UserLoginInfo(lastLogonAt, failedLogins, daysUntilPasswordExpires);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
