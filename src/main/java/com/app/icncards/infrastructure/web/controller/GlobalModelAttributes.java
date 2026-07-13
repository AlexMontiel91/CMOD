package com.app.icncards.infrastructure.web.controller;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;
import com.app.icncards.domain.model.SearchOperator;
import com.app.icncards.infrastructure.security.IdleSessionProperties;

/**
 * Agrega atributos comunes al Model de toda vista, para que las plantillas los
 * lean sin que cada controller los repita:
 *  - idleTimeoutMinutes: para el temporizador de inactividad (idle-timeout.js).
 *  - twoValueOperators: lista (CSV) de nombres de SearchOperator que requieren dos
 *    valores (Entre/No entre), para que folder-search-form.js sepa cuando mostrar
 *    el segundo input sin duplicar la regla del dominio (SearchOperator.requiresTwoValues()).
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

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
}
