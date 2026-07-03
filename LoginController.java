package mx.infotec.imss.infrastructure.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.infrastructure.security.LoginAttemptService;
import mx.infotec.imss.infrastructure.web.form.LoginForm;

/**
 * Login server-rendered. La autenticacion se hace por controller (no por el filtro
 * de Spring Security) para poder:
 *  - validar campos vacios en backend (@Valid), no confiar en el 'required' HTML,
 *  - aplicar el bloqueo de intentos ANTES de mandar el logon a RACF,
 *  - mostrar mensajes por campo y globales desde messages.properties.
 *
 * Tras autenticar, persiste el SecurityContext en la sesion y cambia el id de
 * sesion (anti session-fixation).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginAttemptService loginAttempts;

    @GetMapping("/login")
    public String showLogin(@RequestParam(required = false) String logout,
                            @RequestParam(required = false) String expired,
                            @RequestParam(required = false) String idle,
                            @RequestParam(required = false) String denied,
                            Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        model.addAttribute("logout", logout != null);
        model.addAttribute("expired", expired != null);
        model.addAttribute("idle", idle != null);
        model.addAttribute("denied", denied != null);
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@Valid @ModelAttribute("loginForm") LoginForm form,
                        BindingResult binding,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Model model) {

        // 1) Validacion backend de campos vacios -> errores por campo
        if (binding.hasErrors()) {
            form.setPassword(null); // nunca re-renderizar el password
            return "login";
        }

        String username = form.getUsername().trim();

        // 2) Bloqueo de intentos (protege el userid RACF): corta antes de llegar a RACF
        if (loginAttempts.isBlocked(username)) {
            log.warn("Login bloqueado para usuario '{}' por exceso de intentos", username);
            binding.reject("login.error.locked");
            form.setPassword(null);
            return "login";
        }

        // 3) Autenticacion real (logon contra OnDemand dentro del provider)
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, form.getPassword()));

            // Exito: anti session-fixation + persistir contexto en sesion
            request.getSession();           // asegura que exista
            request.changeSessionId();      // nuevo id, misma sesion
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            loginAttempts.onSuccess(username);
            form.setPassword(null);
            return "redirect:/";

        } catch (BadCredentialsException e) {
            // Fallo de CREDENCIALES: cuenta para el bloqueo
            loginAttempts.onFailure(username);
            binding.reject("login.error.bad");
            form.setPassword(null);
            return "login";

        } catch (AuthenticationException e) {
            // Fallo TECNICO (servicio no disponible): NO cuenta para el bloqueo
            log.warn("Login no disponible para usuario '{}': {}", username, e.getMessage());
            binding.reject("login.error.service");
            form.setPassword(null);
            return "login";
        }
    }
}
