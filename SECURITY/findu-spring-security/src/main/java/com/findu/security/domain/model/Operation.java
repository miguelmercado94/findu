package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Operación/endpoint: {@code path} es el resto de la URL; {@code method} el verbo HTTP; {@code name} la authority.
 */
@Getter
@Setter
public class Operation {

    private Long id;
    private String name;
    /** Ruta relativa al micro (ej. /api/v1/customers). */
    private String path;
    /** GET, POST, PUT, DELETE, … */
    private String method;
    private Long moduleId;
    /**
     * Si es true, la operación no exige usuario autenticado para autorizar el exchange (coincidencia path+método en BD).
     */
    private boolean permiteAll;
    private boolean active = true;
}
