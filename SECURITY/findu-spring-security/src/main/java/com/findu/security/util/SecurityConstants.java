package com.findu.security.util;

import lombok.experimental.UtilityClass;

/**
 * Constantes de seguridad (JWT, etc.). El acceso anónimo a APIs se define en BD ({@code operation.permite_all}),
 * resuelto por {@link com.findu.security.config.security.authorization.FinduReactiveAuthorizationManager}.
 */
@UtilityClass
public class SecurityConstants {

    /** Header con el algoritmo JWT (ej. HS256). Por defecto se usa HS256. */
    public static final String HEADER_JWT_ALGORITHM = "X-JWT-Algorithm";
    public static final String DEFAULT_JWT_ALGORITHM = "HS256";

    /** Valor de {@code typ} en el header JWT para access token (Authorization en APIs). */
    public static final String JWT_HEADER_TYP_ACCESS = "JWT";

    /** Valor de {@code typ} en el header JWT para refresh (solo POST /auth/refresh). */
    public static final String JWT_HEADER_TYP_REFRESH = "JWTRefresh";
}
