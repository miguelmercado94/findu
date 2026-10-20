package com.findu.transaction.domain.model.finance;

import com.findu.transaction.domain.enums.BreBOrderStatus;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
public class BreBPaymentOrder {

    private String id;
    private String orderCode;
    private Long providerId;
    private Money amount;
    private String breBKey;
    private String qrPayload;
    private BreBOrderStatus status;
    private Instant expiresAt;
    private Instant confirmedAt;
    private String externalTransactionId;
    private Instant createdAt;
    private Instant updatedAt;

    private BreBPaymentOrder() {}

    public static BreBPaymentOrder create(
            Long providerId,
            Money amount,
            String breBKey,
            String qrPayload,
            Duration validityDuration) {

        if (providerId == null) throw new IllegalArgumentException("providerId es obligatorio");
        if (amount == null || !amount.isPositive()) throw new IllegalArgumentException("El monto debe ser positivo");
        if (breBKey == null || breBKey.isBlank()) throw new IllegalArgumentException("La llave Bre-B es obligatoria");

        Instant now = Instant.now();
        Duration duration = validityDuration != null ? validityDuration : Duration.ofMinutes(10);

        BreBPaymentOrder order = new BreBPaymentOrder();
        order.id = UUID.randomUUID().toString();
        order.orderCode = "BREB-ORD-" + now.toEpochMilli();
        order.providerId = providerId;
        order.amount = amount;
        order.breBKey = breBKey.trim();
        order.qrPayload = qrPayload != null ? qrPayload : "";
        order.status = BreBOrderStatus.PENDING;
        order.expiresAt = now.plus(duration);
        order.createdAt = now;
        order.updatedAt = now;

        return order;
    }

    public static BreBPaymentOrder reconstitute(
            String id,
            String orderCode,
            Long providerId,
            Money amount,
            String breBKey,
            String qrPayload,
            BreBOrderStatus status,
            Instant expiresAt,
            Instant confirmedAt,
            String externalTransactionId,
            Instant createdAt,
            Instant updatedAt) {

        BreBPaymentOrder order = new BreBPaymentOrder();
        order.id = id;
        order.orderCode = orderCode;
        order.providerId = providerId;
        order.amount = amount;
        order.breBKey = breBKey;
        order.qrPayload = qrPayload;
        order.status = status;
        order.expiresAt = expiresAt;
        order.confirmedAt = confirmedAt;
        order.externalTransactionId = externalTransactionId;
        order.createdAt = createdAt;
        order.updatedAt = updatedAt;
        return order;
    }

    public boolean isExpired() {
        return this.status == BreBOrderStatus.PENDING && Instant.now().isAfter(this.expiresAt);
    }

    public void checkAndMarkExpired() {
        if (isExpired()) {
            this.status = BreBOrderStatus.EXPIRED;
            this.updatedAt = Instant.now();
        }
    }

    public void confirm(String externalTransactionId) {
        if (this.status == BreBOrderStatus.CONFIRMED) {
            return; // Idempotente
        }
        if (isExpired()) {
            this.status = BreBOrderStatus.EXPIRED;
            throw new IllegalStateException("La orden Bre-B ha expirado y no puede confirmarse");
        }
        this.status = BreBOrderStatus.CONFIRMED;
        this.externalTransactionId = externalTransactionId;
        this.confirmedAt = Instant.now();
        this.updatedAt = this.confirmedAt;
    }

    public void cancel() {
        if (this.status == BreBOrderStatus.CONFIRMED) {
            throw new IllegalStateException("No se puede cancelar una orden ya confirmada");
        }
        this.status = BreBOrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
