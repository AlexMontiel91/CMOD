package mx.infotec.imss.domain.model;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Definicion del formulario de busqueda de un folder: sus campos, en el orden
 * provisto por OnDemand. Es lo que arma la pantalla /folder/{nombre}.
 */
@Getter
public class FolderSearchDefinition {

    private final String folderName;
    private final String folderDescription;
    private final List<SearchFieldDefinition> fields;

    public FolderSearchDefinition(String folderName, String folderDescription,
                                  List<SearchFieldDefinition> fields) {
        this.folderName = folderName;
        this.folderDescription = folderDescription;
        this.fields = Collections.unmodifiableList(fields);
    }
}
