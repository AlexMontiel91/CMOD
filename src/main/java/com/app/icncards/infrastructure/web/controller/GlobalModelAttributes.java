package com.app.icncards.infrastructure.web.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;
import com.app.icncards.domain.model.SearchOperator;
import com.app.icncards.domain.model.UserLoginInfo;
import com.app.icncards.infrastructure.security.IdleSessionProperties;
import com.app.icncards.infrastructure.security.OnDemandUserDetails;

/**
 * Agrega atributos comunes al Model de toda vista, para que las plantillas los
 * lean sin que cada controller los repita:
 *  - idleTimeoutMinutes: para el temporizador de inactividad (idle-timeout.js).
 *  - twoValueOperators: lista (CSV) de nombres de SearchOperator que requieren dos
 *    valores (Entre/No entre), para que folder-search-form.js sepa cuando mostrar
 *    el segundo input sin duplicar la regla del dominio (SearchOperator.requiresTwoValues()).
 *  - datos de sesion para el header comun (username, lastLogonFormatted / firstLogin,
 *    failedLogins), leidos del principal autenticado. En vistas sin sesion (login,
 *    error) no se agrega nada.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private static final ZoneId MX_ZONE = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter LAST_LOGON_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // Sin registro previo (null) o fecha sentinel (antes de esta fecha) => primer login.
    private static final Instant FIRST_LOGIN_THRESHOLD = Instant.parse("1980-01-01T00:00:00Z");

    private final IdleSessionProperties idleSessionProperties;

    @ModelAttribute("idleTimeoutMinutes")
    public int idleTimeoutMinutes() {
        return idleSessionProperties.getTimeoutMinutes();
    }

    @ModelAttribute("twoValueOperators")
    public String twoValueOperators() {
        return Arrays.stream(SearchOperator.values())
                .filter(SearchOperator::requiresTwoValues)
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    /**
     * Datos de sesion que el header comun (appHeader) muestra en toda vista
     * autenticada. Se leen del principal; si no hay sesion no se agrega nada (las
     * vistas de login/error no usan el header).
     */
    @ModelAttribute
    public void sessionInfo(Model model) {
        OnDemandUserDetails principal = currentPrincipal();
        if (principal == null) {
            return;
        }
        model.addAttribute("username", principal.getUsername());

        UserLoginInfo info = principal.getLoginInfo();
        if (info == null) {
            return;
        }
        model.addAttribute("failedLogins", info.getFailedLoginsSinceLastSuccess());

        Instant last = info.getLastLogonAt();
        if (last == null || last.isBefore(FIRST_LOGIN_THRESHOLD)) {
            model.addAttribute("firstLogin", true);
        } else {
            model.addAttribute("lastLogonFormatted", LAST_LOGON_FORMAT.format(last.atZone(MX_ZONE)));
        }
    }

    private static OnDemandUserDetails currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OnDemandUserDetails) {
            return (OnDemandUserDetails) auth.getPrincipal();
        }
        return null;
    }
}
