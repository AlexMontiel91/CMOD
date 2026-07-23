package com.app.icncards.infrastructure.web.controller;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.app.icncards.application.service.FolderService;
import com.app.icncards.domain.model.FolderSearchCriterion;
import com.app.icncards.domain.model.FolderSearchDefinition;
import com.app.icncards.domain.model.FolderSearchResult;
import com.app.icncards.domain.model.SearchFieldDefinition;
import com.app.icncards.domain.model.SearchOperator;
import com.app.icncards.infrastructure.odwek.connection.OnDemandException;

/**
 * Pantalla de busqueda de un folder y su ejecucion:
 *  - GET  /folder/{name}         -> formulario dinamico (campos que devuelve OnDemand).
 *  - POST /folder/{name}/search  -> valida requeridos, ejecuta la busqueda (criteria
 *    search) y renderiza la tabla de resultados.
 *
 * Validacion de obligatorios en BACKEND (no se confia solo en el 'required' nativo):
 * si algun campo requerido llega vacio, se re-renderiza el MISMO formulario con el
 * error bajo el campo y todo lo demas repoblado. Nota: hoy solo se valida value1.
 *
 * Errores: InvalidCredentialsException (credencial sellada ya no valida) se deja
 * propagar al GlobalExceptionHandler (invalida sesion + login). OnDemandException
 * (tecnico) se degrada con gracia re-renderizando el formulario con un aviso, para
 * no perder los criterios del usuario.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping("/folder/{folderName}")
    public String showSearchForm(@PathVariable String folderName, Model model) {
        model.addAttribute("definition", folderService.getSearchDefinition(folderName));
        return "folder-search";
    }

    @PostMapping("/folder/{folderName}/search")
    public String search(@PathVariable String folderName,
                         @RequestParam Map<String, String> allParams,
                         Model model) {
        FolderSearchDefinition definition = folderService.getSearchDefinition(folderName);
        Set<String> missingRequired = findMissingRequired(definition, allParams);

        if (!missingRequired.isEmpty()) {
            model.addAttribute("definition", definition);
            model.addAttribute("submittedValues", allParams);   // repobla lo ya escrito
            model.addAttribute("missingFields", missingRequired);
            return "folder-search";
        }

        List<FolderSearchCriterion> criteria = buildCriteria(definition, allParams);
        try {
            FolderSearchResult result = folderService.search(folderName, criteria);
            model.addAttribute("definition", definition);
            model.addAttribute("result", result);
            return "folder-results";

        } catch (OnDemandException e) {
            log.warn("Fallo tecnico al ejecutar la busqueda en folder '{}'", folderName, e);
            model.addAttribute("definition", definition);
            model.addAttribute("submittedValues", allParams);
            model.addAttribute("searchError", true);
            return "folder-search";
        }
    }

    /** Construye los criterios de dominio a partir de los parametros del form + la definicion. */
    private List<FolderSearchCriterion> buildCriteria(FolderSearchDefinition definition,
                                                      Map<String, String> params) {
        List<FolderSearchCriterion> criteria = new ArrayList<>();
        for (SearchFieldDefinition field : definition.getFields()) {
            String value1 = trim(params.get("criteria[" + field.getName() + "].value1"));
            if (value1.isEmpty()) {
                continue; // campo sin valor: no filtra
            }
            SearchOperator operator = resolveOperator(field, params.get("criteria[" + field.getName() + "].operator"));
            String value2 = trim(params.get("criteria[" + field.getName() + "].value2"));
            criteria.add(new FolderSearchCriterion(field.getName(), operator,
                    value1, operator.requiresTwoValues() ? value2 : null));
        }
        return criteria;
    }

    /** Operador enviado, validado contra los validos del campo; si no aplica, el por defecto. */
    private SearchOperator resolveOperator(SearchFieldDefinition field, String opParam) {
        if (opParam != null) {
            try {
                SearchOperator op = SearchOperator.valueOf(opParam);
                if (field.getValidOperators().contains(op)) {
                    return op;
                }
            } catch (IllegalArgumentException ignored) {
                // valor no reconocido: cae al operador por defecto
            }
        }
        return field.getDefaultOperator();
    }

    private Set<String> findMissingRequired(FolderSearchDefinition definition, Map<String, String> params) {
        Set<String> missing = new LinkedHashSet<>();
        for (SearchFieldDefinition field : definition.getFields()) {
            if (!field.isRequired()) {
                continue;
            }
            String value1 = params.get("criteria[" + field.getName() + "].value1");
            if (value1 == null || value1.trim().isEmpty()) {
                missing.add(field.getName());
            }
        }
        return missing;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
