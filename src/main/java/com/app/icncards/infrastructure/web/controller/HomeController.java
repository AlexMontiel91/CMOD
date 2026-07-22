package com.app.icncards.infrastructure.web.controller;

import java.util.Collections;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.app.icncards.application.service.FolderService;
import com.app.icncards.infrastructure.odwek.connection.InvalidCredentialsException;
import com.app.icncards.infrastructure.odwek.connection.OnDemandException;
import com.app.icncards.infrastructure.security.OnDemandUserDetails;
import com.app.icncards.infrastructure.security.PasswordExpiryProperties;

/**
 * Home: muestra los folders asignados al usuario autenticado y el aviso de
 * contrasena por vencer. El usuario y los datos de "ultima conexion" / intentos
 * fallidos los expone {@link GlobalModelAttributes} para el header comun de toda
 * vista autenticada (antes se calculaban aqui).
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

    private final FolderService folderService;
    private final PasswordExpiryProperties passwordExpiryProperties;

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OnDemandUserDetails principal, Model model) {
        addPasswordExpiryWarning(principal, model);

        String username = principal != null ? principal.getUsername() : "";
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

    /** Aviso de contrasena por vencer (dato capturado en el login, dentro del principal). */
    private void addPasswordExpiryWarning(OnDemandUserDetails principal, Model model) {
        if (principal == null || principal.getLoginInfo() == null) {
            return;
        }
        Integer days = principal.getLoginInfo().getDaysUntilPasswordExpires();
        // Defensivo: si el valor viniera negativo (posible sentinel de "no aplica"),
        // no se muestra el aviso.
        if (days != null && days >= 0 && days <= passwordExpiryProperties.getWarnDays()) {
            model.addAttribute("passwordExpiringInDays", days);
        }
    }
}
