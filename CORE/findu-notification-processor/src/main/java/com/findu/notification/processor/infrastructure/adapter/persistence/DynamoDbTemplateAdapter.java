package com.findu.notification.processor.infrastructure.adapter.persistence;

import com.findu.notification.processor.application.port.output.TemplateRepositoryPort;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter de persistencia para plantillas usando DynamoDB.
 * Implementa TemplateRepositoryPort (la capa de servicio no conoce DynamoDB).
 *
 * Tabla: notification_templates
 * PK: id (composite: templateCode#channel#language, ej: "ORDER_DELIVERED#EMAIL#es")
 */
@Component
public class DynamoDbTemplateAdapter implements TemplateRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTemplateAdapter.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbTemplateAdapter(DynamoDbClient dynamoDbClient,
                                   @Value("${findu.notification.templates.table:notification_templates}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public Optional<NotificationTemplate> findByCodeAndChannelAndLanguage(String templateCode, String channel, String language) {
        String compositeId = buildCompositeId(templateCode, channel, language);
        log.debug("DynamoDB get: table={} id={}", tableName, compositeId);

        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("id", AttributeValue.fromS(compositeId)))
                    .build());

            if (!response.hasItem() || response.item().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapToTemplate(response.item()));
        } catch (Exception e) {
            log.error("Error consultando template en DynamoDB: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<NotificationTemplate> findByCodeAndChannel(String templateCode, String channel) {
        // Fallback: buscar con idioma "es"
        return findByCodeAndChannelAndLanguage(templateCode, channel, "es");
    }

    private String buildCompositeId(String templateCode, String channel, String language) {
        return templateCode.toUpperCase() + "#" + channel.toUpperCase() + "#" + language.toLowerCase();
    }

    private NotificationTemplate mapToTemplate(Map<String, AttributeValue> item) {
        return NotificationTemplate.builder()
                .id(getStringOrNull(item, "id"))
                .channel(getStringOrNull(item, "channel"))
                .language(getStringOrNull(item, "language"))
                .contentType(getStringOrNull(item, "content_type"))
                .subject(getStringOrNull(item, "subject"))
                .bodyTemplate(getStringOrNull(item, "body_template"))
                .version(item.containsKey("version") ? Integer.parseInt(item.get("version").n()) : 1)
                .active(!item.containsKey("is_active") || item.get("is_active").bool())
                .build();
    }

    private String getStringOrNull(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).s() : null;
    }
}
