package com.findu.security.util;

import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Constantes de seguridad (rutas públicas, roles, operaciones por defecto, etc.).
 */
@UtilityClass
public class SecurityConstants {

    /**
     * Nombres de operaciones por defecto cuando el rol no tiene operaciones en rol_operation.
     * Se usan como authorities (GrantedAuthority) hasta que se poblen las tablas.
     */
    public static final List<String> DEFAULT_OPERATION_NAMES = List.of(
            "READ_ALL_PRODUCT", "READ_ONE_PRODUCT", "CREATE_PRODUCT", "UPDATE_PRODUCT", "DISABLE_PRODUCT",
            "READ_ALL_CATEGORY", "READ_ONE_CATEGORY", "CREATE_CATEGORY", "UPDATE_CATEGORY", "DISABLE_CATEGORY"
    );

    /** Rutas públicas (sin autenticación). POST /api/v1/customers y POST /api/v1/auth/** se configuran por método en SecurityConfig. */
    public static final String[] PUBLIC_PATHS = {
            "/actuator/health",
            "/api/public/**"
    };

    public static final String API_PUBLIC_PREFIX = "/api/public";

    /** Header con el algoritmo JWT (ej. HS256). Por defecto se usa HS256. */
    public static final String HEADER_JWT_ALGORITHM = "X-JWT-Algorithm";
    public static final String DEFAULT_JWT_ALGORITHM = "HS256";

    /** Valor de {@code typ} en el header JWT para access token (Authorization en APIs). */
    public static final String JWT_HEADER_TYP_ACCESS = "JWT";

    /** Valor de {@code typ} en el header JWT para refresh (solo POST /auth/refresh). */
    public static final String JWT_HEADER_TYP_REFRESH = "JWTRefresh";
}
