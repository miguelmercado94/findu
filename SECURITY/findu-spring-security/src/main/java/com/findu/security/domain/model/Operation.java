package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Modelo de dominio Operation (operación/endpoint).
 */
@Getter
@Setter
public class Operation {

    private Long id;
    private String name;
    private String path;
    private Long moduleId;
    private boolean active = true;
}
