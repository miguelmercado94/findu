package com.findu.notification.processor.infrastructure.adapter.out.persistence.adapter;

import com.findu.notification.processor.application.port.out.persistence.DeviceTokenRepositoryPort;
import com.findu.notification.processor.domain.model.DeviceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter DynamoDB para el store de device tokens.
 *
 * Tabla: {@code findu_device_tokens} (configurable). PK {@code device_token} (S).
 * GSIs: {@code GSI_username} (PK username) y {@code GSI_canonical_recipient} (PK canonical_recipient).
 *
 * upsert = PutItem (idempotente por PK); las consultas usan Query sobre los GSI y devuelven
 * la lista de device_token; las bajas son DeleteItem por device_token. Los errores se tragan
 * con log para no romper el flujo (best-effort).
 */
@Component
public class DynamoDbDeviceTokenAdapter implements DeviceTokenRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbDeviceTokenAdapter.class);

    private static final String ATTR_DEVICE_TOKEN = "device_token";
    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_PLATFORM = "platform";
    private static final String ATTR_CANONICAL_RECIPIENT = "canonical_recipient";
    private static final String ATTR_CREATED_AT = "created_at";
    private static final String ATTR_UPDATED_AT = "updated_at";

    private static final String GSI_USERNAME = "GSI_username";
    private static final String GSI_CANONICAL_RECIPIENT = "GSI_canonical_recipient";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbDeviceTokenAdapter(
            DynamoDbClient dynamoDbClient,
            @Value("${findu.notification.dynamodb.device-tokens-table:findu_device_tokens}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public void upsert(DeviceToken deviceToken) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put(ATTR_DEVICE_TOKEN, AttributeValue.fromS(deviceToken.getDeviceToken()));
            if (deviceToken.getUsername() != null) {
                item.put(ATTR_USERNAME, AttributeValue.fromS(deviceToken.getUsername()));
            }
            if (deviceToken.getPlatform() != null) {
                item.put(ATTR_PLATFORM, AttributeValue.fromS(deviceToken.getPlatform()));
            }
            if (deviceToken.getCanonicalRecipient() != null) {
                item.put(ATTR_CANONICAL_RECIPIENT, AttributeValue.fromS(deviceToken.getCanonicalRecipient()));
            }
            if (deviceToken.getCreatedAt() != null) {
                item.put(ATTR_CREATED_AT, AttributeValue.fromS(deviceToken.getCreatedAt().toString()));
            }
            if (deviceToken.getUpdatedAt() != null) {
                item.put(ATTR_UPDATED_AT, AttributeValue.fromS(deviceToken.getUpdatedAt().toString()));
            }

            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build());

            log.info("Device token upserted in DynamoDB: username={} canonicalRecipient={}",
                    deviceToken.getUsername(), deviceToken.getCanonicalRecipient());
        } catch (Exception e) {
            log.error("Error upserting device token in DynamoDB: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<String> findTokensByCanonicalRecipient(String canonicalRecipient) {
        return queryTokensByIndex(GSI_CANONICAL_RECIPIENT, ATTR_CANONICAL_RECIPIENT, canonicalRecipient);
    }

    @Override
    public List<String> findTokensByUsername(String username) {
        return queryTokensByIndex(GSI_USERNAME, ATTR_USERNAME, username);
    }

    private List<String> queryTokensByIndex(String indexName, String keyAttr, String keyValue) {
        List<String> tokens = new ArrayList<>();
        if (keyValue == null || keyValue.isBlank()) {
            return tokens;
        }
        try {
            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(tableName)
                    .indexName(indexName)
                    .keyConditionExpression("#k = :v")
                    .expressionAttributeNames(Map.of("#k", keyAttr))
                    .expressionAttributeValues(Map.of(":v", AttributeValue.fromS(keyValue)))
                    .build());

            for (Map<String, AttributeValue> item : response.items()) {
                AttributeValue token = item.get(ATTR_DEVICE_TOKEN);
                if (token != null && token.s() != null) {
                    tokens.add(token.s());
                }
            }
        } catch (Exception e) {
            log.error("Error querying device tokens by {}={}: {}", keyAttr, keyValue, e.getMessage(), e);
        }
        return tokens;
    }

    @Override
    public void delete(String username, String deviceToken) {
        deleteByToken(deviceToken);
    }

    @Override
    public void deleteByToken(String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) {
            return;
        }
        try {
            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(ATTR_DEVICE_TOKEN, AttributeValue.fromS(deviceToken)))
                    .build());
            log.info("Device token deleted from DynamoDB");
        } catch (Exception e) {
            log.error("Error deleting device token from DynamoDB: {}", e.getMessage(), e);
        }
    }
}
