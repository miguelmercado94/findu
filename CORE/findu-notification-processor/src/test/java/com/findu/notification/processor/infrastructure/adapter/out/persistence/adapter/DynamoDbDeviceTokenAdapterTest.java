package com.findu.notification.processor.infrastructure.adapter.out.persistence.adapter;

import com.findu.notification.processor.domain.model.DeviceToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamoDbDeviceTokenAdapterTest {

    private DynamoDbClient dynamoDbClient;
    private DynamoDbDeviceTokenAdapter adapter;

    @BeforeEach
    void setUp() {
        dynamoDbClient = mock(DynamoDbClient.class);
        adapter = new DynamoDbDeviceTokenAdapter(dynamoDbClient, "findu_device_tokens");
    }

    @Test
    @DisplayName("upsert escribe un PutItem con device_token (PK), username, platform y canonical_recipient")
    void upsert_writesPutItemWithExpectedAttributes() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        DeviceToken token = DeviceToken.builder()
                .username("juan")
                .deviceToken("tok-123")
                .platform("ANDROID")
                .canonicalRecipient("PROVEEDOR_77")
                .createdAt(now)
                .updatedAt(now)
                .build();

        adapter.upsert(token);

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(captor.capture());
        Map<String, AttributeValue> item = captor.getValue().item();
        assertThat(captor.getValue().tableName()).isEqualTo("findu_device_tokens");
        assertThat(item.get("device_token").s()).isEqualTo("tok-123");
        assertThat(item.get("username").s()).isEqualTo("juan");
        assertThat(item.get("platform").s()).isEqualTo("ANDROID");
        assertThat(item.get("canonical_recipient").s()).isEqualTo("PROVEEDOR_77");
        assertThat(item.get("created_at").s()).isEqualTo(now.toString());
        assertThat(item.get("updated_at").s()).isEqualTo(now.toString());
    }

    @Test
    @DisplayName("findTokensByCanonicalRecipient consulta el GSI_canonical_recipient y devuelve los device_token")
    void findTokensByCanonicalRecipient_queriesGsiAndReturnsTokens() {
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(QueryResponse.builder()
                .items(
                        Map.of("device_token", AttributeValue.fromS("tok-a")),
                        Map.of("device_token", AttributeValue.fromS("tok-b")))
                .build());

        List<String> tokens = adapter.findTokensByCanonicalRecipient("PROVEEDOR_77");

        assertThat(tokens).containsExactly("tok-a", "tok-b");
        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(dynamoDbClient).query(captor.capture());
        assertThat(captor.getValue().indexName()).isEqualTo("GSI_canonical_recipient");
        assertThat(captor.getValue().expressionAttributeNames()).containsEntry("#k", "canonical_recipient");
        assertThat(captor.getValue().expressionAttributeValues().get(":v").s()).isEqualTo("PROVEEDOR_77");
    }

    @Test
    @DisplayName("findTokensByUsername consulta el GSI_username")
    void findTokensByUsername_queriesUsernameGsi() {
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(QueryResponse.builder()
                .items(Map.of("device_token", AttributeValue.fromS("tok-x")))
                .build());

        List<String> tokens = adapter.findTokensByUsername("juan");

        assertThat(tokens).containsExactly("tok-x");
        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(dynamoDbClient).query(captor.capture());
        assertThat(captor.getValue().indexName()).isEqualTo("GSI_username");
        assertThat(captor.getValue().expressionAttributeNames()).containsEntry("#k", "username");
        assertThat(captor.getValue().expressionAttributeValues().get(":v").s()).isEqualTo("juan");
    }

    @Test
    @DisplayName("delete borra por device_token (PK)")
    void delete_deletesByDeviceToken() {
        adapter.delete("juan", "tok-123");

        ArgumentCaptor<DeleteItemRequest> captor = ArgumentCaptor.forClass(DeleteItemRequest.class);
        verify(dynamoDbClient).deleteItem(captor.capture());
        assertThat(captor.getValue().key().get("device_token").s()).isEqualTo("tok-123");
    }

    @Test
    @DisplayName("deleteByToken borra por device_token (PK)")
    void deleteByToken_deletesByDeviceToken() {
        adapter.deleteByToken("tok-999");

        ArgumentCaptor<DeleteItemRequest> captor = ArgumentCaptor.forClass(DeleteItemRequest.class);
        verify(dynamoDbClient).deleteItem(captor.capture());
        assertThat(captor.getValue().key().get("device_token").s()).isEqualTo("tok-999");
    }
}
