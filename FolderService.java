package mx.infotec.imss.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mx.infotec.imss.application.port.out.FolderRepository;
import mx.infotec.imss.domain.model.FolderSearchDefinition;
import mx.infotec.imss.domain.model.FolderSummary;

/**
 * Caso de uso: folders asignados y su definicion de busqueda. Punto de extension
 * natural para reglas futuras (orden preferido, filtros, cache).
 */
@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;

    public List<FolderSummary> listAssignedFolders() {
        return folderRepository.findAssignedFolders();
    }

    public FolderSearchDefinition getSearchDefinition(String folderName) {
        return folderRepository.findSearchDefinition(folderName);
    }
}
