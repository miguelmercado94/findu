package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Módulo funcional. {@code pathBase} identifica al micro en la URI (ej. security-auth).
 */
@Getter
@Setter
public class Modulo {

    private Long id;
    private String name;
    /** Segmento del micro (descubrimiento / gateway); debe coincidir con {@code spring.webflux.base-path}. */
    private String pathBase;
    private boolean active = true;
}
