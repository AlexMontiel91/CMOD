package mx.infotec.imss.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandCredentials;

/**
 * Puente entre el contexto de seguridad y el template de OnDemand. Los adaptadores
 * de negocio (p. ej. CmodDocumentRepository) piden aqui la credencial del usuario
 * autenticado para pasarla a onDemand.execute(...), sin conocer detalles de sesion
 * ni de cifrado.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserCredentials {

    private final CredentialCipher cipher;

    /** Credencial del usuario autenticado en la peticion actual. */
    public OnDemandCredentials current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof OnDemandUserDetails)) {
            throw new IllegalStateException("No hay un usuario OnDemand autenticado en el contexto");
        }
        OnDemandUserDetails principal = (OnDemandUserDetails) auth.getPrincipal();
        return new SessionBackedCredentials(principal.getSessionCredential(), cipher);
    }
}
