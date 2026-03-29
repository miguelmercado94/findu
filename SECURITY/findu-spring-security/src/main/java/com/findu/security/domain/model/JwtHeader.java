package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Modelo de dominio: cabecera JWT (alg, typ, kid, customClaims).
 */
@Getter
@Setter
public class JwtHeader {

    private String alg;
    private String typ;
    private String kid;
    private Map<String, Object> customClaims;
}
