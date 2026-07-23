package com.app.icncards.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import com.app.icncards.application.port.out.FolderRepository;
import com.app.icncards.domain.model.FolderSearchCriterion;
import com.app.icncards.domain.model.FolderSearchDefinition;
import com.app.icncards.domain.model.FolderSearchResult;
import com.app.icncards.domain.model.FolderSummary;

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

    public FolderSearchResult search(String folderName, java.util.List<FolderSearchCriterion> criteria) {
        return folderRepository.search(folderName, criteria);
    }
}
