package com.findu.notification.adapter;

import com.findu.notification.model.NotificationRecord;
import com.findu.notification.port.NotificationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementación de NotificationRepositoryPort usando DynamoDB.
 * Si se quisiera cambiar a MongoDB o PostgreSQL, se crea otro adapter sin tocar el service.
 */
@Component
public class DynamoDbNotificationRepository implements NotificationRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbNotificationRepository.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbNotificationRepository(DynamoDbClient dynamoDbClient,
                                          @Value("${findu.notification.dynamodb.table-name:findu_notifications}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public void save(NotificationRecord record) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("notification_id", AttributeValue.fromS(record.getNotificationId()));
            item.put("type", AttributeValue.fromS(record.getType()));
            item.put("recipient", AttributeValue.fromS(record.getRecipient()));
            item.put("template_code", AttributeValue.fromS(record.getTemplateCode()));
            item.put("language", AttributeValue.fromS(record.getLanguage()));
            item.put("dispatched", AttributeValue.fromBool(record.isDispatched()));
            item.put("requires_connection", AttributeValue.fromBool(record.isRequiresConnection()));
            item.put("created_at", AttributeValue.fromS(record.getCreatedAt()));

            if (record.getParams() != null) {
                Map<String, AttributeValue> paramsMap = new HashMap<>();
                record.getParams().forEach((k, v) -> paramsMap.put(k, AttributeValue.fromS(v != null ? v : "")));
                item.put("params", AttributeValue.fromM(paramsMap));
            }

            if (record.getError() != null) {
                item.put("error", AttributeValue.fromS(record.getError()));
            }

            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build());

            log.info("Notification saved to DynamoDB: id={} type={} recipient={}",
                    record.getNotificationId(), record.getType(), record.getRecipient());
        } catch (Exception e) {
            log.error("Error saving notification to DynamoDB: {}", e.getMessage(), e);
        }
    }
}
