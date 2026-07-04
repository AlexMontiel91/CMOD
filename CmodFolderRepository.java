package mx.infotec.imss.infrastructure.odwek.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.ibm.edms.od.ODCriteria;
import com.ibm.edms.od.ODFolder;
import com.ibm.edms.od.ODServer;

import lombok.RequiredArgsConstructor;
import mx.infotec.imss.application.port.out.FolderRepository;
import mx.infotec.imss.domain.model.FolderSearchDefinition;
import mx.infotec.imss.domain.model.FolderSummary;
import mx.infotec.imss.domain.model.SearchFieldDefinition;
import mx.infotec.imss.domain.model.SearchFieldKind;
import mx.infotec.imss.domain.model.SearchOperator;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandOperations;
import mx.infotec.imss.infrastructure.security.CurrentUserCredentials;

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
}
