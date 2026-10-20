package com.findu.transaction.infrastructure.adapter.out.event;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.event.*;
import com.findu.transaction.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEventPublisherTest {

    private InMemoryTransactionEventPublisherAdapter publisher;

    @BeforeEach
    void setUp() {
        publisher = new InMemoryTransactionEventPublisherAdapter("http://localhost:8088");
        publisher.clearPublishedEvents();
    }

    @Test
    @DisplayName("Debe formatear y publicar un PaymentCompletedEvent en el envelope correcto")
    void publish_PaymentCompletedEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "PAY-100",
                "TX-200",
                300L,
                400L,
                new Money(new BigDecimal("150000.00")),
                PaymentMethod.CARD,
                "REF-PAY-001",
                Instant.now()
        );

        FinancialEventEnvelope envelope = event.toEnvelope("CORR-999");
        publisher.publish(envelope);

        assertThat(publisher.getPublishedEvents()).hasSize(1);
        FinancialEventEnvelope published = publisher.getPublishedEvents().get(0);

        assertThat(published.getEventId()).isNotBlank();
        assertThat(published.getEventType()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(published.getEventVersion()).isEqualTo(1);
        assertThat(published.getProducer()).isEqualTo("findu-transaction");
        assertThat(published.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(published.getCorrelationId()).isEqualTo("CORR-999");

        Map<String, Object> payload = published.getPayload();
        assertThat(payload).containsEntry("paymentId", "PAY-100");
        assertThat(payload).containsEntry("transactionId", "TX-200");
        assertThat(payload).containsEntry("customerId", 300L);
        assertThat(payload).containsEntry("providerId", 400L);
        assertThat(payload).containsEntry("amount", new BigDecimal("150000.00"));
        assertThat(payload).containsEntry("currency", "COP");

        // Verificar que no contenga información sensible
        assertThat(payload).doesNotContainKey("pan");
        assertThat(payload).doesNotContainKey("cvv");
        assertThat(payload).doesNotContainKey("token");
    }

    @Test
    @DisplayName("Debe formatear y publicar RefundCompletedEvent")
    void publish_RefundCompletedEvent() {
        RefundCompletedEvent event = new RefundCompletedEvent(
                "REFUND-1",
                "TX-200",
                300L,
                new Money(new BigDecimal("50000.00")),
                "REFUND-REF-01",
                Instant.now()
        );

        FinancialEventEnvelope envelope = event.toEnvelope("CORR-888");
        publisher.publish(envelope);

        assertThat(publisher.getPublishedEvents()).hasSize(1);
        FinancialEventEnvelope published = publisher.getPublishedEvents().get(0);
        assertThat(published.getEventType()).isEqualTo("REFUND_COMPLETED");
        assertThat(published.getAggregateType()).isEqualTo("REFUND");
        assertThat(published.getPayload()).containsEntry("refundId", "REFUND-1");
    }

    @Test
    @DisplayName("Debe formatear y publicar PayoutCompletedEvent")
    void publish_PayoutCompletedEvent() {
        PayoutCompletedEvent event = new PayoutCompletedEvent(
                "PAYOUT-1",
                "SETT-1",
                400L,
                new Money(new BigDecimal("200000.00")),
                "EXT-PAYOUT-01",
                Instant.now()
        );

        FinancialEventEnvelope envelope = event.toEnvelope("CORR-777");
        publisher.publish(envelope);

        assertThat(publisher.getPublishedEvents()).hasSize(1);
        FinancialEventEnvelope published = publisher.getPublishedEvents().get(0);
        assertThat(published.getEventType()).isEqualTo("PAYOUT_COMPLETED");
        assertThat(published.getAggregateType()).isEqualTo("PAYOUT");
    }
}
