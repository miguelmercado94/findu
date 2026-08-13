package com.findu.notification.processor.infrastructure.adapter.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.notification.processor.application.port.output.NotificationStatusRepositoryPort;
import com.findu.notification.processor.domain.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class DynamoDbNotificationStatusAdapter implements NotificationStatusRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbNotificationStatusAdapter.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final ObjectMapper objectMapper;

    public DynamoDbNotificationStatusAdapter(
            DynamoDbClient dynamoDbClient,
            @Value("${findu.notification.dynamodb.table-name:findu_notifications}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void saveSuccess(String notificationId) {
        try {
            Map<String, AttributeValue> key = Map.of("notification_id", AttributeValue.fromS(notificationId));
            
            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put("status", AttributeValueUpdate.builder()
                    .value(AttributeValue.fromS("SUCCESS"))
                    .action(AttributeAction.PUT)
                    .build());
            updates.put("processed_at", AttributeValueUpdate.builder()
                    .value(AttributeValue.fromS(Instant.now().toString()))
                    .action(AttributeAction.PUT)
                    .build());

            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .attributeUpdates(updates)
                    .build());

            log.info("Notification status updated to SUCCESS in DynamoDB: id={}", notificationId);
        } catch (Exception e) {
            log.error("Error updating notification success status in DynamoDB: {}", e.getMessage(), e);
        }
    }

    @Override
    public void saveFailure(NotificationMessage message, String errorCode, String errorDesc, int retries) {
        try {
            Map<String, AttributeValue> key = Map.of("notification_id", AttributeValue.fromS(message.getNotificationId()));
            
            String eventJson = objectMapper.writeValueAsString(message);
            
            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put("status", AttributeValueUpdate.builder()
                    .value(AttributeValue.fromS("FAILED"))
                    .action(AttributeAction.PUT)
                    .build());
            updates.put("event_json", AttributeValueUpdate.builder()
                    .value(AttributeValue.fromS(eventJson))
                    .action(AttributeAction.PUT)
                    .build());
            updates.put("error_code", AttributeValueUpdate.builder()
                    .value(AttributeValue.fromS(errorCode != null ? errorCode : "PROCESS_ERROR"))
                    .action(AttributeAction.PUT)
                    .build());
            updates.put("error_description", AttributeValueUpdate.builder()
                    .value(AttributeValue.fromS(errorDesc != null ? errorDesc : "Unknown processing error"))
                    .action(AttributeAction.PUT)
                    .build());
            updates.put("retries", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().n(String.valueOf(retries)).build())
                    .action(AttributeAction.PUT)
                    .build());
            updates.put("processed_at", AttributeValueUpdate.builder()
                    .value(AttributeValue.fromS(Instant.now().toString()))
                    .action(AttributeAction.PUT)
                    .build());

            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .attributeUpdates(updates)
                    .build());

            log.info("Notification status updated to FAILED in DynamoDB: id={} error={}", message.getNotificationId(), errorDesc);
        } catch (Exception e) {
            log.error("Error updating notification failure status in DynamoDB: {}", e.getMessage(), e);
        }
    }
}
