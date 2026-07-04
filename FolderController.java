package mx.infotec.imss.infrastructure.web.controller;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.infotec.imss.application.service.FolderService;
import mx.infotec.imss.domain.model.FolderSearchDefinition;
import mx.infotec.imss.domain.model.SearchFieldDefinition;

/**
 * Pantalla de busqueda de un folder: renderiza el formulario dinamico segun los
 * campos que devuelve OnDemand para ese folder (variable por folder).
 *
 * Validacion de obligatorios: se hace en BACKEND (no se confia solo en el
 * 'required' nativo, que puede desactivarse); si algun campo requerido llega
 * vacio, se re-renderiza el MISMO formulario con el error bajo el campo y con
 * todo lo demas que el usuario ya habia escrito repoblado (no se pierde su
 * trabajo). Nota: hoy solo se valida value1; un campo requerido con operador
 * "Entre" y value2 vacio no se marca como incompleto (extension futura si hace
 * falta un caso real).
 *
 * El POST de este incremento SOLO recibe y loguea los criterios validos (confirma
 * que el naming de los campos esta bien cableado); la EJECUCION real de la
 * busqueda contra OnDemand (abrir folder, setCriteria, search(), paginar hits) es
 * el siguiente incremento.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping("/folder/{folderName}")
    public String showSearchForm(@PathVariable String folderName,
                                 @RequestParam(required = false) String submitted,
                                 Model model) {
        model.addAttribute("definition", folderService.getSearchDefinition(folderName));
        model.addAttribute("submitted", submitted != null);
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

        // Placeholder: confirma que los criterios llegan bien nombrados.
        // La ejecucion real (setCriteria + search()) es el siguiente incremento.
        log.info("Busqueda recibida para folder '{}' con {} parametros (pendiente de ejecutar): {}",
                folderName, allParams.size(), allParams.keySet());
        String encoded = UriUtils.encodePathSegment(folderName, StandardCharsets.UTF_8);
        return "redirect:/folder/" + encoded + "?submitted";
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
}
