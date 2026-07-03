package mx.infotec.imss.infrastructure.web.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;
import mx.infotec.imss.infrastructure.security.IdleSessionProperties;

/**
 * Agrega idleTimeoutMinutes al Model de toda vista, para que el temporizador de
 * inactividad (idle-timeout.js) lea el valor sin que cada controller lo repita.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final IdleSessionProperties idleSessionProperties;

    @ModelAttribute("idleTimeoutMinutes")
    public int idleTimeoutMinutes() {
        return idleSessionProperties.getTimeoutMinutes();
    }
}
