package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Modelo de dominio Rol.
 */
@Getter
@Setter
public class Rol {

    private Integer id;
    private String name;
    private boolean active = true;
}
