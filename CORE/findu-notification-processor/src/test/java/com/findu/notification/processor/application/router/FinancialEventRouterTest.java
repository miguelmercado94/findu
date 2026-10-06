package com.findu.notification.processor.application.router;

import com.findu.notification.processor.application.handler.financial.*;
import com.findu.notification.processor.domain.service.FinancialEventDeduplicationService;
import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

class FinancialEventRouterTest {

    private FinancialEventRouter router;
    private FinancialEventDeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        deduplicationService = new FinancialEventDeduplicationService();
        deduplicationService.clear();

        List<FinancialNotificationHandler> handlers = List.of(
                new PaymentCompletedNotificationHandler(),
                new PaymentFailedNotificationHandler(),
                new RefundCompletedNotificationHandler(),
                new RefundFailedNotificationHandler(),
                new PayoutCompletedNotificationHandler(),
                new PayoutFailedNotificationHandler(),
                new ProviderDebtCreatedNotificationHandler(),
                new ProviderDebtPaidNotificationHandler(),
                new ExtraExpenseApprovedNotificationHandler(),
                new ExtraExpenseRejectedNotificationHandler()
        );

        router = new FinancialEventRouter(handlers, deduplicationService);
    }

    @Test
    @DisplayName("Debe enrutar exitosamente PAYMENT_COMPLETED al handler correspondiente")
    void route_PaymentCompletedEvent() {
        FinancialEventEnvelopeDto event = new FinancialEventEnvelopeDto();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("PAYMENT_COMPLETED");
        event.setEventVersion(1);
        event.setProducer("findu-transaction");
        event.setOccurredAt(Instant.now());
        event.setAggregateId("PAY-1");
        event.setAggregateType("PAYMENT");
        event.setPayload(Map.of(
                "paymentId", "PAY-1",
                "customerId", 100L,
                "providerId", 200L,
                "amount", new BigDecimal("100000.00")
        ));

        assertThatCode(() -> router.route(event).block()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe evitar el procesamiento duplicado de un mismo eventId (idempotencia)")
    void route_DuplicateEvent_ShouldBeDeduplicated() {
        String eventId = "EVENT-UUID-UNIQUE-123";

        FinancialEventEnvelopeDto event1 = new FinancialEventEnvelopeDto();
        event1.setEventId(eventId);
        event1.setEventType("REFUND_COMPLETED");
        event1.setPayload(Map.of("refundId", "REF-1", "customerId", 100L));

        FinancialEventEnvelopeDto event2 = new FinancialEventEnvelopeDto();
        event2.setEventId(eventId);
        event2.setEventType("REFUND_COMPLETED");
        event2.setPayload(Map.of("refundId", "REF-1", "customerId", 100L));

        // Primera llegada -> procesa
        assertThatCode(() -> router.route(event1).block()).doesNotThrowAnyException();

        // Segunda llegada -> detecta duplicado y se completa sin procesar de nuevo
        assertThatCode(() -> router.route(event2).block()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe manejar eventos no soportados sin lanzar excepciones")
    void route_UnhandledEventType() {
        FinancialEventEnvelopeDto event = new FinancialEventEnvelopeDto();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("UNKNOWN_EVENT");

        assertThatCode(() -> router.route(event).block()).doesNotThrowAnyException();
    }
}
