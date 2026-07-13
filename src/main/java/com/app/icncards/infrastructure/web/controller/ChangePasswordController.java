package com.app.icncards.infrastructure.web.controller;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.app.icncards.infrastructure.odwek.connection.InvalidCredentialsException;
import com.app.icncards.infrastructure.odwek.connection.OnDemandException;
import com.app.icncards.infrastructure.odwek.connection.OnDemandOperations;
import com.app.icncards.infrastructure.web.form.ChangePasswordForm;

/**
 * Cambio de contrasena vencida (RACF), usando el logon de 4 argumentos de ODWEK.
 * Ruta publica (no requiere sesion): el usuario llega aqui precisamente porque no
 * pudo iniciar sesion normal (su password vencio).
 *
 * La contrasena actual (vencida) se vuelve a pedir en este formulario en vez de
 * propagarla desde el intento de login fallido: evita mantener texto plano de la
 * credencial en sesion/redirect entre peticiones.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChangePasswordController {

    private final OnDemandOperations onDemand;

    @GetMapping("/change-password")
    public String showForm(@RequestParam(required = false) String user, Model model) {
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute("changePasswordForm", new ChangePasswordForm(user));
        }
        return "change-password";
    }

    @PostMapping("/change-password")
    public String submit(@Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
                         BindingResult binding, Model model) {

        // 1) Validacion backend de campos vacios
        if (binding.hasErrors()) {
            clearPasswords(form);
            return "change-password";
        }

        // 2) Las dos contrasenas nuevas deben coincidir
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            binding.reject("changePassword.error.mismatch");
            clearPasswords(form);
            return "change-password";
        }

        char[] oldPwd = form.getCurrentPassword().toCharArray();
        char[] newPwd = form.getNewPassword().toCharArray();
        try {
            // changeExpiredPassword limpia oldPwd/newPwd internamente tras usarlos
            onDemand.changeExpiredPassword(form.getUser(), oldPwd, newPwd);

            log.info("Password actualizado exitosamente para usuario '{}'", form.getUser());
            return "redirect:/login?passwordChanged";

        } catch (InvalidCredentialsException e) {
            // La contrasena ACTUAL (vencida) que escribio no es correcta
            log.info("Cambio de password rechazado para '{}': contrasena actual invalida", form.getUser());
            binding.reject("changePassword.error.badCurrent");
            clearPasswords(form);
            return "change-password";

        } catch (OnDemandException e) {
            // Tecnico, o la nueva contrasena no cumple la politica de RACF (errorId
            // aun no mapeado; revisar [ODWEK errorId] en el log si esto se repite)
            log.warn("No se pudo cambiar el password para '{}'", form.getUser(), e);
            binding.reject("changePassword.error.service");
            clearPasswords(form);
            return "change-password";
        }
    }

    /** Nunca se re-renderizan las contrasenas escritas. */
    private void clearPasswords(ChangePasswordForm form) {
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setConfirmPassword(null);
    }
}
