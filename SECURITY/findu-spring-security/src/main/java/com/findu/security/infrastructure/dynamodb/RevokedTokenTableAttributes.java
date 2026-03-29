package com.findu.security.infrastructure.dynamodb;

/**
 * Atributos de la tabla de sesiones de token (DynamoDB).
 */
public final class RevokedTokenTableAttributes {

    /** Partition key: SHA-256 hex del access JWT. */
    public static final String ACCESS_TOKEN_HASH = "access_token_hash";
    /** GSI partition key: SHA-256 hex del refresh JWT. */
    public static final String REFRESH_TOKEN_HASH = "refresh_token_hash";
    public static final String JWT = "jwt";
    public static final String JWT_REFRESH = "jwt_refresh";
    public static final String AVAILABLE = "available";
    /** TTL nativo DynamoDB (epoch segundos). */
    public static final String TTL = "ttl";

    /** Nombre del GSI por refresh_token_hash. */
    public static final String GSI_REFRESH_TOKEN_HASH = "refresh_token_hash_index";

    /** Esquema legado (solo blacklist por hash). */
    public static final String TOKEN_HASH_LEGACY = "token_hash";
    public static final String TOKEN_TYPE_LEGACY = "token_type";

    private RevokedTokenTableAttributes() {
    }
}
