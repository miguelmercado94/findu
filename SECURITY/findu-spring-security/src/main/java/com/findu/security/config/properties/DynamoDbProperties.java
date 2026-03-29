package com.findu.security.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración DynamoDB (LocalStack o AWS). Revocación de tokens JWT.
 */
@ConfigurationProperties(prefix = "findu.aws.dynamodb")
public record DynamoDbProperties(
        boolean enabled,
        String endpoint,
        String region,
        String revokedTokensTable,
        boolean createTableIfNotExists
) {
}
