package com.app.icncards.domain.model;

import lombok.Value;

/**
 * Representa un folder de OnDemand tal como lo ve el negocio: nombre y
 * descripcion. Sin dependencias de ODWEK ni de Spring (POJO de dominio puro).
 */
@Value
public class FolderSummary {
    String name;
    String description;
}
