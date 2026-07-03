package mx.infotec.imss.application.port.out;

import java.util.List;

import mx.infotec.imss.domain.model.FolderSummary;

/**
 * Puerto de salida (dominio). No conoce ODWEK ni credenciales: eso es detalle del
 * adaptador que lo implemente. "Usuario actual" se resuelve dentro del adaptador
 * a partir del contexto de seguridad de la peticion.
 */
public interface FolderRepository {

    /** Folders a los que el usuario autenticado en la peticion actual tiene acceso. */
    List<FolderSummary> findAssignedFolders();
}
