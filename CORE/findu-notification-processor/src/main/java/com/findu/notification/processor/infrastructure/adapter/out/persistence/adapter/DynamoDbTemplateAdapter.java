package com.findu.notification.processor.infrastructure.adapter.out.persistence.adapter;

import com.findu.notification.processor.application.port.out.persistence.TemplateRepositoryPort;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.exception.InvalidTemplateException;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import com.findu.notification.processor.infrastructure.adapter.out.persistence.entity.NotificationTemplateItem;
import com.findu.notification.processor.infrastructure.adapter.out.persistence.mapper.NotificationTemplateItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter de persistencia para plantillas usando DynamoDB (esquema versionado §9).
 * Implementa {@link TemplateRepositoryPort} — la capa de servicio no conoce DynamoDB.
 *
 * <p>Tabla: {@code notification_templates}. PK {@code id = CODE#TIPO#LANG};
 * SK {@code sk}: {@code v#<n>} para versiones inmutables y {@code v#ACTIVE} como
 * puntero desnormalizado (resuelve el envío en un único {@code GetItem}).</p>
 */
@Component
public class DynamoDbTemplateAdapter implements TemplateRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTemplateAdapter.class);

    private final DynamoDbClient dynamoDbClient;
    private final NotificationTemplateItemMapper mapper;
    private final String tableName;
    private final String tipoIndexName;

    public DynamoDbTemplateAdapter(DynamoDbClient dynamoDbClient,
                                   NotificationTemplateItemMapper mapper,
                                   @Value("${findu.notification.templates.table:notification_templates}") String tableName,
                                   @Value("${findu.notification.templates.gsi-tipo:GSI_tipo}") String tipoIndexName) {
        this.dynamoDbClient = dynamoDbClient;
        this.mapper = mapper;
        this.tableName = tableName;
        this.tipoIndexName = tipoIndexName;
    }

    // ====================== Hot path de envío ======================

    @Override
    public Optional<NotificationTemplate> findActive(String codigo, TemplateType tipo, String language) {
        String id = NotificationTemplateItem.buildId(codigo, tipo.name(), language);
        log.debug("DynamoDB get activo: table={} id={} sk={}", tableName, id, NotificationTemplateItem.SK_ACTIVE);

        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key(id, NotificationTemplateItem.SK_ACTIVE))
                    .build());

            if (!response.hasItem() || response.item().isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(mapper.toDomain(response.item()));
        } catch (Exception e) {
            log.error("Error resolviendo template activo en DynamoDB (id={}): {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    // ====================== Compatibilidad flujo de envío legacy ======================

    @Override
    public Optional<NotificationTemplate> findByCodeAndChannelAndLanguage(String templateCode, String channel, String language) {
        return findActive(templateCode, channelToTipo(channel), language);
    }

    @Override
    public Optional<NotificationTemplate> findByCodeAndChannel(String templateCode, String channel) {
        return findByCodeAndChannelAndLanguage(templateCode, channel, "es");
    }

    private TemplateType channelToTipo(String channel) {
        // El canal técnico PUSH se resuelve como el tipo funcional NOTIFICACION; el
        // resto coincide 1:1 con el nombre del TemplateType.
        String normalized = channel == null ? "" : channel.toUpperCase();
        if ("PUSH".equals(normalized)) {
            return TemplateType.NOTIFICACION;
        }
        return TemplateType.valueOf(normalized);
    }

    // ====================== Operaciones de administración (§9) ======================

    @Override
    public void createNewVersion(NotificationTemplate tpl) {
        if (tpl == null || tpl.getCodigo() == null || tpl.getTipo() == null || tpl.getLanguage() == null) {
            throw new InvalidTemplateException("codigo, tipo y language son requeridos para crear una versión");
        }

        String id = NotificationTemplateItem.buildId(tpl.getCodigo(), tpl.getTipo().name(), tpl.getLanguage());
        int nextVersion = currentActiveVersion(id) + 1;

        tpl.setId(id);
        tpl.setVersion(nextVersion);
        tpl.setActive(true);

        // Ítem inmutable de la versión concreta.
        tpl.setSk(NotificationTemplateItem.versionSk(nextVersion));
        Map<String, AttributeValue> versionItem = mapper.toItem(tpl);

        // Puntero v#ACTIVE: copia desnormalizada + active_version.
        tpl.setSk(NotificationTemplateItem.SK_ACTIVE);
        Map<String, AttributeValue> activeItem = new HashMap<>(mapper.toItem(tpl));
        activeItem.put(NotificationTemplateItem.ACTIVE_VERSION, AttributeValue.fromN(String.valueOf(nextVersion)));

        try {
            dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                    .transactItems(
                            putItem(versionItem),
                            putItem(activeItem))
                    .build());
            log.info("Nueva versión de plantilla creada: id={} version={}", id, nextVersion);
        } catch (Exception e) {
            log.error("Error creando nueva versión de plantilla (id={}): {}", id, e.getMessage(), e);
            throw new InvalidTemplateException("No se pudo crear la versión de la plantilla: " + e.getMessage());
        }
    }

    @Override
    public void activateVersion(String id, int version) {
        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key(id, NotificationTemplateItem.versionSk(version)))
                    .build());

            if (!response.hasItem() || response.item().isEmpty()) {
                throw new InvalidTemplateException("No existe la versión v#" + version + " para id=" + id);
            }

            Map<String, AttributeValue> activeItem = new HashMap<>(response.item());
            activeItem.put(NotificationTemplateItem.SK, AttributeValue.fromS(NotificationTemplateItem.SK_ACTIVE));
            activeItem.put(NotificationTemplateItem.ACTIVE_VERSION, AttributeValue.fromN(String.valueOf(version)));
            activeItem.put(NotificationTemplateItem.IS_ACTIVE, AttributeValue.fromBool(true));

            dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                    .transactItems(putItem(activeItem))
                    .build());
            log.info("Versión activa repuntada: id={} version={}", id, version);
        } catch (InvalidTemplateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error activando versión (id={} version={}): {}", id, version, e.getMessage(), e);
            throw new InvalidTemplateException("No se pudo activar la versión: " + e.getMessage());
        }
    }

    @Override
    public List<NotificationTemplate> findHistory(String id) {
        try {
            Map<String, AttributeValue> values = new HashMap<>();
            values.put(":id", AttributeValue.fromS(id));
            values.put(":prefix", AttributeValue.fromS(NotificationTemplateItem.SK_PREFIX));

            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("#id = :id AND begins_with(#sk, :prefix)")
                    .expressionAttributeNames(Map.of(
                            "#id", NotificationTemplateItem.ID,
                            "#sk", NotificationTemplateItem.SK))
                    .expressionAttributeValues(values)
                    .build());

            List<NotificationTemplate> history = new ArrayList<>();
            for (Map<String, AttributeValue> item : response.items()) {
                // Excluir el puntero v#ACTIVE: el histórico son las versiones inmutables.
                String sk = item.containsKey(NotificationTemplateItem.SK)
                        ? item.get(NotificationTemplateItem.SK).s() : null;
                if (NotificationTemplateItem.SK_ACTIVE.equals(sk)) {
                    continue;
                }
                history.add(mapper.toDomain(item));
            }
            return history;
        } catch (Exception e) {
            log.error("Error consultando histórico (id={}): {}", id, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<NotificationTemplate> findByTipo(TemplateType tipo) {
        try {
            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(tableName)
                    .indexName(tipoIndexName)
                    .keyConditionExpression("#tipo = :tipo")
                    .expressionAttributeNames(Map.of("#tipo", NotificationTemplateItem.TIPO))
                    .expressionAttributeValues(Map.of(":tipo", AttributeValue.fromS(tipo.name())))
                    .build());

            List<NotificationTemplate> result = new ArrayList<>();
            for (Map<String, AttributeValue> item : response.items()) {
                String sk = item.containsKey(NotificationTemplateItem.SK)
                        ? item.get(NotificationTemplateItem.SK).s() : null;
                // El GSI por tipo lista la versión activa de cada plantilla.
                if (sk == null || NotificationTemplateItem.SK_ACTIVE.equals(sk)) {
                    result.add(mapper.toDomain(item));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Error consultando plantillas por tipo (tipo={}): {}", tipo, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public void deactivate(String id) {
        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key(id, NotificationTemplateItem.SK_ACTIVE))
                    .updateExpression("SET #active = :false")
                    .expressionAttributeNames(Map.of("#active", NotificationTemplateItem.IS_ACTIVE))
                    .expressionAttributeValues(Map.of(":false", AttributeValue.fromBool(false)))
                    .build());
            log.info("Plantilla desactivada: id={}", id);
        } catch (Exception e) {
            log.error("Error desactivando plantilla (id={}): {}", id, e.getMessage(), e);
            throw new InvalidTemplateException("No se pudo desactivar la plantilla: " + e.getMessage());
        }
    }

    // ====================== Helpers ======================

    private int currentActiveVersion(String id) {
        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key(id, NotificationTemplateItem.SK_ACTIVE))
                    .build());
            if (!response.hasItem() || response.item().isEmpty()) {
                return 0;
            }
            Map<String, AttributeValue> item = response.item();
            if (item.containsKey(NotificationTemplateItem.ACTIVE_VERSION)
                    && item.get(NotificationTemplateItem.ACTIVE_VERSION).n() != null) {
                return Integer.parseInt(item.get(NotificationTemplateItem.ACTIVE_VERSION).n());
            }
            if (item.containsKey(NotificationTemplateItem.VERSION)
                    && item.get(NotificationTemplateItem.VERSION).n() != null) {
                return Integer.parseInt(item.get(NotificationTemplateItem.VERSION).n());
            }
            return 0;
        } catch (Exception e) {
            log.warn("No se pudo leer la versión activa (id={}), se asume 0: {}", id, e.getMessage());
            return 0;
        }
    }

    private Map<String, AttributeValue> key(String id, String sk) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(NotificationTemplateItem.ID, AttributeValue.fromS(id));
        key.put(NotificationTemplateItem.SK, AttributeValue.fromS(sk));
        return key;
    }

    private TransactWriteItem putItem(Map<String, AttributeValue> item) {
        return TransactWriteItem.builder()
                .put(Put.builder().tableName(tableName).item(item).build())
                .build();
    }
}
