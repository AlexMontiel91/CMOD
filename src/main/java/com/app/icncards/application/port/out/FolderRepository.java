package com.app.icncards.application.port.out;

import java.util.List;

import com.app.icncards.domain.model.FolderSearchCriterion;
import com.app.icncards.domain.model.FolderSearchDefinition;
import com.app.icncards.domain.model.FolderSearchResult;
import com.app.icncards.domain.model.FolderSummary;

/**
 * Puerto de salida (dominio). No conoce ODWEK ni credenciales: eso es detalle del
 * adaptador que lo implemente. "Usuario actual" se resuelve dentro del adaptador
 * a partir del contexto de seguridad de la peticion.
 */
public interface FolderRepository {

    /** Folders a los que el usuario autenticado en la peticion actual tiene acceso. */
    List<FolderSummary> findAssignedFolders();

    /** Definicion del formulario de busqueda (campos, tipos, operadores) de un folder. */
    FolderSearchDefinition findSearchDefinition(String folderName);

    /** Ejecuta la busqueda del folder con los criterios dados y devuelve los resultados. */
    FolderSearchResult search(String folderName, List<FolderSearchCriterion> criteria);
}
