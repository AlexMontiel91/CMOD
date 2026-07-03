package mx.infotec.imss.infrastructure.web.controller;

import java.security.Principal;
import java.util.Collections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.application.service.FolderService;
import mx.infotec.imss.infrastructure.odwek.connection.InvalidCredentialsException;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandException;

/**
 * Home: muestra los folders asignados al usuario autenticado.
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

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        String username = principal != null ? principal.getName() : "";
        model.addAttribute("username", username);

        try {
            model.addAttribute("folders", folderService.listAssignedFolders());

        } catch (InvalidCredentialsException e) {
            throw e; // el GlobalExceptionHandler decide (invalida sesion + redirige a login)

        } catch (OnDemandException e) {
            log.warn("No se pudieron obtener los folders de '{}': fallo tecnico contra OnDemand", username, e);
            model.addAttribute("folders", Collections.emptyList());
            model.addAttribute("folderError", true);
        }

        return "home";
    }
}

