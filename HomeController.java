package mx.infotec.imss.infrastructure.web.controller;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.application.service.FolderService;
import mx.infotec.imss.domain.model.UserLoginInfo;
import mx.infotec.imss.infrastructure.odwek.connection.InvalidCredentialsException;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandException;
import mx.infotec.imss.infrastructure.security.OnDemandUserDetails;
import mx.infotec.imss.infrastructure.security.PasswordExpiryProperties;

/**
 * Home: muestra los folders asignados al usuario autenticado, y de forma
 * complementaria su "ultima conexion", intentos fallidos y aviso de contrasena
 * por vencer (capturados durante el login, sin llamada extra a OnDemand).
 *
 * InvalidCredentialsException (credencial sellada ya no valida a media sesion) NO
 * se captura aqui: se deja propagar al GlobalExceptionHandler, que centraliza el
 * "invalidar sesion + redirigir a login" para toda la app.
 *
 * OnDemandException (tecnico) SI se captura localmente, porque en esta pantalla
 * conviene degradar con gracia (mostrar la tabla vacia con un aviso) en vez de
 * mandar a la pagina de error generica.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final ZoneId MX_ZONE = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter LAST_LOGON_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FolderService folderService;
    private final PasswordExpiryProperties passwordExpiryProperties;

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OnDemandUserDetails principal, Model model) {
        String username = principal != null ? principal.getUsername() : "";
        model.addAttribute("username", username);
        addLoginInfo(principal, model);

        try {
            model.addAttribute("folders", folderService.listAssignedFolders());

        } catch (InvalidCredentialsException e) {
            throw e; // el GlobalExceptionHandler decide (invalida sesion y redirige a login)

        } catch (OnDemandException e) {
            log.warn("No se pudieron obtener los folders de '{}': fallo tecnico contra OnDemand", username, e);
            model.addAttribute("folders", Collections.emptyList());
            model.addAttribute("folderError", true);
        }

        return "home";
    }

    /** Expone ultima conexion (formateada), intentos fallidos y aviso de password. */
    private void addLoginInfo(OnDemandUserDetails principal, Model model) {
        if (principal == null || principal.getLoginInfo() == null) {
            return;
        }
        UserLoginInfo info = principal.getLoginInfo();
        model.addAttribute("loginInfo", info);

        if (isFirstLogin(info.getLastLogonAt())) {
            // Sin registro previo (null) o fecha sentinel (epoca): se trata como
            // "primer inicio de sesion". No se puede distinguir con certeza un
            // primer login real de un dato no disponible por otra razon; verificar
            // con una cuenta real que nunca haya iniciado sesion.
            model.addAttribute("firstLogin", true);
        } else {
            model.addAttribute("lastLogonFormatted",
                    LAST_LOGON_FORMAT.format(info.getLastLogonAt().atZone(MX_ZONE)));
        }

        Integer days = info.getDaysUntilPasswordExpires();
        // Defensivo: si el valor viniera negativo (posible sentinel de "no aplica"),
        // no se muestra el aviso. Verificar con una cuenta real que valores retorna
        // ODUser.getNumDaysUntilPWExp() cuando la contrasena no expira.
        if (days != null && days >= 0 && days <= passwordExpiryProperties.getWarnDays()) {
            model.addAttribute("passwordExpiringInDays", days);
        }
    }

    /** null, o fecha sentinel tipica de "sin registro previo" (epoca UNIX). */
    private boolean isFirstLogin(java.time.Instant lastLogonAt) {
        return lastLogonAt == null || lastLogonAt.isBefore(java.time.Instant.parse("1980-01-01T00:00:00Z"));
    }
}
