package mx.infotec.imss.infrastructure.web.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home minimo para validar el flujo de sesion de punta a punta (login -> home ->
 * logout). Se reemplazara por el dashboard real en incrementos posteriores.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        model.addAttribute("username", principal != null ? principal.getName() : "");
        return "home";
    }
}
