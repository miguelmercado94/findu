package com.findu.notification.processor.infrastructure.adapter.out.persistence.adapter;

import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import com.findu.notification.processor.infrastructure.adapter.out.persistence.entity.NotificationTemplateItem;
import com.findu.notification.processor.infrastructure.adapter.out.persistence.mapper.NotificationTemplateItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamoDbTemplateAdapterTest {

    private DynamoDbClient dynamoDbClient;
    private DynamoDbTemplateAdapter adapter;

    @BeforeEach
    void setUp() {
        dynamoDbClient = mock(DynamoDbClient.class);
        adapter = new DynamoDbTemplateAdapter(
                dynamoDbClient,
                new NotificationTemplateItemMapper(),
                "notification_templates",
                "GSI_tipo");
    }

    @Test
    @DisplayName("findActive resuelve por GetItem con SK=v#ACTIVE y PK=CODE#TIPO#LANG")
    void findActive_usesActivePointer() {
        Map<String, AttributeValue> item = Map.of(
                NotificationTemplateItem.ID, AttributeValue.fromS("VERIFICACION_OTP#SMS#es"),
                NotificationTemplateItem.SK, AttributeValue.fromS("v#ACTIVE"),
                NotificationTemplateItem.TIPO, AttributeValue.fromS("SMS"),
                NotificationTemplateItem.BODY_TEMPLATE, AttributeValue.fromS("codigo {{otp}}"));
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(item).build());

        Optional<NotificationTemplate> result = adapter.findActive("VERIFICACION_OTP", TemplateType.SMS, "es");

        assertThat(result).isPresent();
        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        org.mockito.Mockito.verify(dynamoDbClient).getItem(captor.capture());
        assertThat(captor.getValue().key().get(NotificationTemplateItem.ID).s())
                .isEqualTo("VERIFICACION_OTP#SMS#es");
        assertThat(captor.getValue().key().get(NotificationTemplateItem.SK).s()).isEqualTo("v#ACTIVE");
    }

    @Test
    @DisplayName("createNewVersion escribe v#<n+1> y v#ACTIVE en una sola transacción")
    void createNewVersion_writesVersionAndActivePointer() {
        // No existe versión activa previa -> active_version = 0 -> nueva versión = 1
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());
        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
                .thenReturn(TransactWriteItemsResponse.builder().build());

        NotificationTemplate tpl = NotificationTemplate.builder()
                .codigo("VERIFICACION_OTP")
                .tipo(TemplateType.SMS)
                .language("es")
                .bodyTemplate("codigo {{otp}}")
                .variables(List.of("otp"))
                .build();

        adapter.createNewVersion(tpl);

        ArgumentCaptor<TransactWriteItemsRequest> captor =
                ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        org.mockito.Mockito.verify(dynamoDbClient).transactWriteItems(captor.capture());

        List<TransactWriteItem> items = captor.getValue().transactItems();
        assertThat(items).hasSize(2);

        List<String> sks = items.stream()
                .map(it -> it.put().item().get(NotificationTemplateItem.SK).s())
                .toList();
        assertThat(sks).containsExactlyInAnyOrder("v#1", "v#ACTIVE");

        // El puntero v#ACTIVE lleva active_version=1
        TransactWriteItem active = items.stream()
                .filter(it -> "v#ACTIVE".equals(it.put().item().get(NotificationTemplateItem.SK).s()))
                .findFirst().orElseThrow();
        assertThat(active.put().item().get(NotificationTemplateItem.ACTIVE_VERSION).n()).isEqualTo("1");
    }
}
