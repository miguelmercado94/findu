package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Modelo de dominio: payload JWT (sub, iat, exp, iss, aud, claims).
 */
@Getter
@Setter
public class JwtPayload {

    private String sub;
    private Long iat;
    private Long exp;
    private String iss;
    private String aud;
    private Map<String, Object> claims;
}
