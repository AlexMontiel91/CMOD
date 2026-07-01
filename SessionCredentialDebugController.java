package mx.infotec.imss.infrastructure.web.devtest;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.infotec.imss.infrastructure.security.OnDemandUserDetails;
import mx.infotec.imss.infrastructure.security.SessionCredential;

/**
 * *** CLASE DESECHABLE — SOLO PARA VALIDAR EL CIFRADO EN DEV. BORRAR ANTES DE MERGEAR. ***
 *
 * Muestra el SessionCredential que vive en la sesion del usuario autenticado, para
 * comprobar que el password NO esta en claro: solo veras el userid y bytes cifrados
 * (iv + cipherText en Base64). Esta clase NO descifra nada.
 *
 * Se activa SOLO con el perfil "sec-debug", nunca por accidente:
 *   --spring.profiles.active=dev,sec-debug
 * Requiere estar autenticado (cae bajo anyRequest().authenticated()).
 */
@RestController
@Profile("sec-debug")
public class SessionCredentialDebugController {

    @GetMapping("/debug/session-credential")
    public Map<String, Object> dump() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> out = new LinkedHashMap<>();

        if (auth == null || !(auth.getPrincipal() instanceof OnDemandUserDetails)) {
            out.put("estado", "sin usuario OnDemand autenticado");
            return out;
        }

        OnDemandUserDetails principal = (OnDemandUserDetails) auth.getPrincipal();
        SessionCredential sc = principal.getSessionCredential();

        Base64.Encoder b64 = Base64.getEncoder();
        out.put("usuario", sc.getUser());
        out.put("ivBase64", b64.encodeToString(sc.getIv()));
        out.put("ivBytes", sc.getIv().length);
        out.put("cipherTextBase64", b64.encodeToString(sc.getCipherText()));
        out.put("cipherTextBytes", sc.getCipherText().length);
        out.put("nota", "Si aqui NO aparece el password legible, el sellado AES-GCM funciona.");
        return out;
    }
}
