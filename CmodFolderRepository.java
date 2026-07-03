package mx.infotec.imss.infrastructure.odwek.adapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.ibm.edms.od.ODServer;

import lombok.RequiredArgsConstructor;
import mx.infotec.imss.application.port.out.FolderRepository;
import mx.infotec.imss.domain.model.FolderSummary;
import mx.infotec.imss.infrastructure.odwek.connection.OnDemandOperations;
import mx.infotec.imss.infrastructure.security.CurrentUserCredentials;

/**
 * Adaptador de salida (hexagonal): implementa {@link FolderRepository} hablando
 * con OnDemand via {@link OnDemandOperations}. Aqui, y solo aqui dentro de este
 * caso de uso, se conocen los tipos de ODWEK (Enumeration, ODServer).
 *
 * La credencial del usuario actual la resuelve CurrentUserCredentials (capa de
 * seguridad), no el puerto de dominio: FolderRepository no sabe que existen
 * credenciales.
 *
 * NOTA (verificar con el servidor real): si OnDemand tiene definidos folders con
 * nombre especifico por idioma, el Javadoc de ODServer indica que hay que llamar
 * getFolders() ANTES de getFolderDescription(), o la descripcion puede fallar con
 * "folder not found". Si tu servidor NO usa folders por idioma (caso comun), este
 * codigo funciona tal cual. Si los usa, avisa para ajustar la secuencia.
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
}
