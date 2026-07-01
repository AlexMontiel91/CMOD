package mx.infotec.imss.infrastructure.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Sirve la pantalla de login. El POST del formulario lo procesa Spring Security
 * (no este controller). Traduce los flags ?error y ?logout a atributos del modelo
 * para que la plantilla FreeMarker los muestre sin depender del motor.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        model.addAttribute("error", error != null);
        model.addAttribute("logout", logout != null);
        return "login";
    }
}
