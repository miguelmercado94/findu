package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Modelo de dominio: JWT compuesto por header, payload y signer.
 */
@Getter
@Setter
public class Jwt {

    private JwtHeader header;
    private JwtPayload payload;
    private JwtSigner signer;
}
