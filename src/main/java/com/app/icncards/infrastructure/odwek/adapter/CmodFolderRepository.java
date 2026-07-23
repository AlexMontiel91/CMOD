package com.app.icncards.infrastructure.odwek.adapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.springframework.stereotype.Repository;

import com.ibm.edms.od.ODCriteria;
import com.ibm.edms.od.ODFolder;
import com.ibm.edms.od.ODHit;
import com.ibm.edms.od.ODServer;

import lombok.RequiredArgsConstructor;
import com.app.icncards.application.port.out.FolderRepository;
import com.app.icncards.domain.model.FolderSearchCriterion;
import com.app.icncards.domain.model.FolderSearchDefinition;
import com.app.icncards.domain.model.FolderSearchResult;
import com.app.icncards.domain.model.FolderSearchRow;
import com.app.icncards.domain.model.FolderSummary;
import com.app.icncards.domain.model.SearchFieldDefinition;
import com.app.icncards.domain.model.SearchFieldKind;
import com.app.icncards.domain.model.SearchOperator;
import com.app.icncards.infrastructure.odwek.connection.OnDemandOperations;
import com.app.icncards.infrastructure.security.CurrentUserCredentials;

/**
 * Adaptador de salida (hexagonal): implementa {@link FolderRepository} hablando
 * con OnDemand via {@link OnDemandOperations}. Aqui, y solo aqui dentro de este
 * caso de uso, se conocen los tipos de ODWEK (Enumeration, ODServer, ODCriteria).
 *
 * La credencial del usuario actual la resuelve CurrentUserCredentials (capa de
 * seguridad), no el puerto de dominio: FolderRepository no sabe que existen
 * credenciales.
 *
 * NOTA (verificar con el servidor real): si OnDemand tiene definidos folders con
 * nombre especifico por idioma, el Javadoc de ODServer indica que hay que llamar
 * getFolders() ANTES de openFolder()/getFolderDescription(), o puede fallar con
 * "folder not found". Si tu servidor NO usa folders por idioma (caso comun), este
 * codigo funciona tal cual.
 */
@Repository
@RequiredArgsConstructor
public class CmodFolderRepository implements FolderRepository {

    private final OnDemandOperations onDemand;
    private final CurrentUserCredentials currentUser;

    @Override
    public List<FolderSummary> findAssignedFolders() {
        return onDemand.execute(currentUser.current(), this::loadFolders);
    }

    @Override
    public FolderSearchDefinition findSearchDefinition(String folderName) {
        return onDemand.execute(currentUser.current(), server -> loadSearchDefinition(server, folderName));
    }

    @Override
    public FolderSearchResult search(String folderName, List<FolderSearchCriterion> criteria) {
        return onDemand.execute(currentUser.current(), server -> runSearch(server, folderName, criteria));
    }

    private List<FolderSummary> loadFolders(ODServer server) throws Exception {
        List<FolderSummary> folders = new ArrayList<>();
        Enumeration<?> names = server.getFolderNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String description = server.getFolderDescription(name);
            folders.add(new FolderSummary(name, description));
        }
        folders.sort(Comparator.comparing(FolderSummary::getName, String.CASE_INSENSITIVE_ORDER));
        return folders;
    }

    private FolderSearchDefinition loadSearchDefinition(ODServer server, String folderName) throws Exception {
        ODFolder folder = server.openFolder(folderName);
        try {
            List<SearchFieldDefinition> fields = new ArrayList<>();

            // getDisplayOrder(): orden configurado por el administrador de OnDemand.
            // NOTA: el ejemplo oficial mas simple de IBM (TcListCriteria, pensado para
            // "compare results to Windows Client") usa el orden natural de getCriteria()
            // sin reordenar; puede que en la practica coincidan. Verificar con un folder
            // real de varios campos si hay diferencia visible contra el cliente.
            String[] displayOrder = folder.getDisplayOrder();
            for (String name : displayOrder) {
                ODCriteria criteria = folder.getCriteria(name);
                if (criteria == null) {
                    continue; // defensivo: no deberia pasar si el nombre viene del propio folder
                }
                // isDisplayable(): "Determine whether this criterion may be displayed to
                // users" (confirmado en IBM Redbooks SG24-7646, tabla de metodos de
                // ODCriteria). Es el metodo correcto para "campos de referencia que el
                // cliente de IBM no muestra" -- NO confundir con isQueryable() ("puede
                // usarse para acotar una busqueda"), que es un concepto distinto y NO
                // determina si el campo debe verse en el formulario.
                if (!criteria.isDisplayable()) {
                    continue;
                }
                fields.add(toFieldDefinition(criteria));
            }
            return new FolderSearchDefinition(folder.getName(), folder.getDescription(), fields);
        } finally {
            folder.close();
        }
    }

    private SearchFieldDefinition toFieldDefinition(ODCriteria criteria) throws Exception {
        SearchFieldKind kind = ODFieldKindMapper.resolve(criteria);

        List<SearchOperator> validOperators = new ArrayList<>();
        for (int code : criteria.getValidOperators()) {
            SearchOperator operator = ODOperatorCodec.fromCode(code);
            if (operator != null) { // ignora silenciosamente operadores que esta UI aun no soporta
                validOperators.add(operator);
            }
        }
        SearchOperator defaultOperator = ODOperatorCodec.fromCode(criteria.getDefaultOperator());

        // getSearchValues() es un PAR FIJO [valorPorDefecto1, valorPorDefecto2], no una
        // lista de N valores -- confirmado por el ejemplo oficial de IBM (TcListCriteria),
        // que accede a value_vec[0] y value_vec[1] sin verificar longitud.
        String[] searchValues = criteria.getSearchValues();
        String defaultValue1 = (searchValues != null && searchValues.length > 0) ? searchValues[0] : null;
        String defaultValue2 = (searchValues != null && searchValues.length > 1) ? searchValues[1] : null;

        List<String> choiceValues = new ArrayList<>();
        if (kind == SearchFieldKind.CHOICE) {
            String[] fixedValues = criteria.getFixedValues();
            if (fixedValues != null) {
                Collections.addAll(choiceValues, fixedValues);
            }
        }

        // getDefaultFmt(): formato que ODWEK espera para el VALOR de busqueda
        // (p. ej. "%m/%d/%y"). Distinto de getDisplayFmt() (formato de despliegue).
        String dateFormat = (kind == SearchFieldKind.DATE || kind == SearchFieldKind.DATETIME)
                ? criteria.getDefaultFmt()
                : null;

        // Los inputs HTML type=date / datetime-local SOLO aceptan ISO (yyyy-MM-dd /
        // yyyy-MM-ddTHH:mm). Los valores por defecto de OnDemand vienen en el formato
        // del folder (dateFormat), asi que hay que convertirlos a ISO o el navegador
        // los ignora y el campo aparece vacio. Ante cualquier fallo se deja el valor
        // original (mismo comportamiento que antes: campo vacio, sin romper).
        if (kind == SearchFieldKind.DATE || kind == SearchFieldKind.DATETIME) {
            boolean withTime = (kind == SearchFieldKind.DATETIME);
            defaultValue1 = toIsoDefault(defaultValue1, dateFormat, withTime);
            defaultValue2 = toIsoDefault(defaultValue2, dateFormat, withTime);
        }

        return new SearchFieldDefinition(
                criteria.getName(),
                criteria.getDescription(),
                kind,
                criteria.isRequired(),
                defaultOperator,
                validOperators,
                defaultValue1,
                defaultValue2,
                choiceValues,
                criteria.getMaxEntryChars(),
                dateFormat);
    }

    /**
     * Convierte un valor por defecto de fecha desde el formato de OnDemand (strftime,
     * p. ej. "%m/%d/%y") al ISO que exigen los inputs HTML (yyyy-MM-dd para date;
     * yyyy-MM-ddTHH:mm para datetime-local). Si no se puede parsear (formato no
     * reconocido, valor vacio, etc.) devuelve el valor original: el campo quedara
     * vacio como antes, sin romper el render.
     */
    private static String toIsoDefault(String raw, String odFormat, boolean withTime) {
        if (raw == null || raw.trim().isEmpty() || odFormat == null || odFormat.isEmpty()) {
            return raw;
        }
        try {
            DateTimeFormatter parser = DateTimeFormatter.ofPattern(strftimeToJava(odFormat), Locale.ROOT);
            if (withTime) {
                return LocalDateTime.parse(raw.trim(), parser)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            }
            return LocalDate.parse(raw.trim(), parser).toString(); // ISO yyyy-MM-dd
        } catch (Exception e) {
            return raw;
        }
    }

    /** Traduce los especificadores strftime mas comunes al patron de DateTimeFormatter. */
    private static String strftimeToJava(String f) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (c == '%' && i + 1 < f.length()) {
                char n = f.charAt(++i);
                switch (n) {
                    case 'Y': sb.append("yyyy"); break;
                    case 'y': sb.append("yy");   break;
                    case 'm': sb.append("MM");   break;
                    case 'd': sb.append("dd");   break;
                    case 'e': sb.append("d");    break;
                    case 'H': sb.append("HH");   break;
                    case 'I': sb.append("hh");   break;
                    case 'M': sb.append("mm");   break;
                    case 'S': sb.append("ss");   break;
                    case 'p': sb.append("a");    break;
                    case 'j': sb.append("DDD");  break;
                    case 'b': sb.append("MMM");  break;
                    case 'B': sb.append("MMMM"); break;
                    case 'a': sb.append("EEE");  break;
                    case 'A': sb.append("EEEE"); break;
                    case '%': sb.append('%');    break;
                    default:  sb.append(n);      break; // desconocido: literal
                }
            } else if (Character.isLetter(c)) {
                sb.append('\'').append(c).append('\''); // literal a escapar en el patron Java
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Ejecuta la busqueda (criteria search, la recomendada por IBM): fija operador y
     * valores en cada ODCriteria del folder y llama search(). Las columnas salen de
     * getDisplayOrder() y cada hit aporta sus display values + docId. searchCountHits()
     * da el total real (best-effort) para el aviso "primeros N de M".
     */
    private FolderSearchResult runSearch(ODServer server, String folderName,
                                         List<FolderSearchCriterion> criteria) throws Exception {
        ODFolder folder = server.openFolder(folderName);
        try {
            for (FolderSearchCriterion criterion : criteria) {
                ODCriteria c = folder.getCriteria(criterion.getFieldName());
                if (c == null) {
                    continue; // defensivo: el campo ya no existe en el folder
                }
                c.setOperator(ODOperatorCodec.toCode(criterion.getOperator()));

                // Fechas: el form envia ISO; ODWEK espera el formato del folder
                // (getDefaultFmt). Es el espejo de toIsoDefault.
                SearchFieldKind kind = ODFieldKindMapper.resolve(c);
                boolean isDate = kind == SearchFieldKind.DATE || kind == SearchFieldKind.DATETIME;
                boolean withTime = kind == SearchFieldKind.DATETIME;
                String v1 = isDate ? toOnDemandDate(criterion.getValue1(), c.getDefaultFmt(), withTime)
                                   : criterion.getValue1();
                if (criterion.getOperator().requiresTwoValues()) {
                    String v2 = isDate ? toOnDemandDate(criterion.getValue2(), c.getDefaultFmt(), withTime)
                                       : criterion.getValue2();
                    c.setSearchValues(v1, v2);
                } else {
                    c.setSearchValue(v1);
                }
            }

            long total = safeCountHits(folder);   // total real sin traer datos (best-effort)
            Vector<?> hits = folder.search();      // trae hasta maxHits (tope de ODConfig/folder)
            String message = trimToEmpty(folder.getSearchMessage());
            String[] columns = folder.getDisplayOrder();

            List<FolderSearchRow> rows = new ArrayList<>(hits.size());
            for (Object element : hits) {
                ODHit hit = (ODHit) element;
                List<String> values = new ArrayList<>(columns.length);
                for (String column : columns) {
                    String value = hit.getDisplayValue(column);
                    values.add(value != null ? value : "");
                }
                rows.add(new FolderSearchRow(values, hit.getDocId()));
            }

            int shown = rows.size();
            int totalHits = total >= shown ? (int) total : shown; // sin conteo: usar lo mostrado
            boolean truncated = totalHits > shown;
            return new FolderSearchResult(Arrays.asList(columns), rows, totalHits, shown, truncated, message);
        } finally {
            folder.close();
        }
    }

    /** searchCountHits (cuenta sin traer datos, devuelve long) es best-effort: si falla, -1. */
    private static long safeCountHits(ODFolder folder) {
        try {
            return folder.searchCountHits();
        } catch (Exception e) {
            return -1L;
        }
    }

    /** Convierte un valor ISO del form al formato de fecha del folder (getDefaultFmt) que
     *  espera setSearchValue; ante cualquier fallo devuelve el valor original. */
    private static String toOnDemandDate(String iso, String odFormat, boolean withTime) {
        if (iso == null || iso.trim().isEmpty() || odFormat == null || odFormat.isEmpty()) {
            return iso;
        }
        try {
            DateTimeFormatter target = DateTimeFormatter.ofPattern(strftimeToJava(odFormat), Locale.ROOT);
            if (withTime) {
                return LocalDateTime.parse(iso.trim()).format(target);
            }
            return LocalDate.parse(iso.trim()).format(target);
        } catch (Exception e) {
            return iso;
        }
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
