package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Modelo de dominio Módulo.
 */
@Getter
@Setter
public class Modulo {

    private Long id;
    private String name;
    private String pathBase;
    private boolean active = true;
}
