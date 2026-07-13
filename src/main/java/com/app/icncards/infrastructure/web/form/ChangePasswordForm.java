package com.app.icncards.infrastructure.web.form;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Formulario de cambio de contrasena vencida. No se valida una politica de
 * complejidad especifica (mayusculas, digitos, etc.) porque no sabemos con
 * certeza la politica real de RACF; solo se valida backend que los campos no
 * esten vacios. RACF es la autoridad final: si la nueva contrasena no cumple su
 * politica, el ODException correspondiente se traduce a un mensaje generico
 * (capturar el errorId del log [ODWEK errorId] si aparece, para mapearlo mejor).
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordForm {

    private String user;

    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String confirmPassword;

    public ChangePasswordForm(String user) {
        this.user = user;
    }
}
