package mx.infotec.imss.infrastructure.web.form;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Formulario de login. La validacion de campos vacios se hace en el BACKEND con
 * @NotBlank (no se confia en el 'required' del HTML, que se puede desactivar).
 * Los mensajes se resuelven por codigo (NotBlank.loginForm.campo) desde
 * messages.properties.
 */
@Getter
@Setter
public class LoginForm {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
