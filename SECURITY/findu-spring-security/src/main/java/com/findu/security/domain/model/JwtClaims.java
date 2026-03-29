package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Claims extraídas al parsear un JWT (sub, iat, exp, iss, aud, claims).
 */
@Getter
@Setter
public class JwtClaims {

    private String sub;
    private Long iat;
    private Long exp;
    private String iss;
    private String aud;
    private Map<String, Object> claims;
}
