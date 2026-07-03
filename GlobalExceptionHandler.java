package mx.infotec.imss.infrastructure.web.advice;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.infrastructure.odwek.connection.InvalidCredentialsException;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandException;

/**
 * Manejo centralizado de excepciones para comportamiento CONSISTENTE en toda la
 * app (nunca se repite controller por controller):
 *
 *  - InvalidCredentialsException: la credencial sellada ya no es valida a media
 *    sesion (p. ej. password cambio en RACF). Se invalida la sesion y se manda a
 *    re-login. NUNCA se reintenta (riesgo de revocar el userid en RACF).
 *  - OnDemandException (tecnico), sin capturar localmente: pagina generica de
 *    error, sin exponer detalles internos (OWASP A09: no leak de stack traces).
 *  - Exception generica: red de seguridad final; se loguea completo en servidor,
 *    al usuario solo un mensaje generico.
 *
 * Un controller especifico puede seguir capturando OnDemandException LOCALMENTE
 * si prefiere degradar con gracia dentro de su propia vista (p. ej. HomeController
 * muestra la tabla vacia con un aviso, en vez de mandar a la pagina de error). En
 * ese caso este handler global simplemente no se activa para esa excepcion.
 *
 * NOTA: InvalidCredentialsException extiende OnDemandException; Spring elige el
 * @ExceptionHandler mas especifico automaticamente, asi que el orden de los
 * metodos abajo no importa para la resolucion.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public String handleInvalidCredentials(InvalidCredentialsException e, HttpServletRequest request) {
        log.info("Credencial invalida a media sesion en '{}'; se cierra la sesion", request.getRequestURI());
        request.getSession().invalidate();
        return "redirect:/login?expired";
    }

    @ExceptionHandler(OnDemandException.class)
    public String handleOnDemandTechnical(OnDemandException e, Model model) {
        log.warn("Fallo tecnico contra OnDemand no manejado localmente por el controller", e);
        model.addAttribute("messageCode", "error.service.unavailable");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception e, Model model) {
        log.error("Error no controlado", e);
        model.addAttribute("messageCode", "error.unexpected");
        return "error";
    }
}
