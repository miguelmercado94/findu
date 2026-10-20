package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.BreBPaymentOrderRepository;
import com.findu.transaction.domain.enums.BreBOrderStatus;
import com.findu.transaction.domain.model.finance.BreBPaymentOrder;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.BreBPaymentOrderEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataBreBPaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaBreBPaymentOrderAdapter implements BreBPaymentOrderRepository {

    private final SpringDataBreBPaymentOrderRepository jpaRepository;

    @Override
    public BreBPaymentOrder save(BreBPaymentOrder order) {
        BreBPaymentOrderEntity entity = toEntity(order);
        BreBPaymentOrderEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<BreBPaymentOrder> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<BreBPaymentOrder> findByOrderCode(String orderCode) {
        return jpaRepository.findByOrderCode(orderCode).map(this::toDomain);
    }

    @Override
    public Optional<BreBPaymentOrder> findByBreBKey(String breBKey) {
        return jpaRepository.findByBreBKey(breBKey).map(this::toDomain);
    }

    @Override
    public List<BreBPaymentOrder> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private BreBPaymentOrderEntity toEntity(BreBPaymentOrder order) {
        return BreBPaymentOrderEntity.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .providerId(order.getProviderId())
                .amount(order.getAmount().getAmount())
                .breBKey(order.getBreBKey())
                .qrPayload(order.getQrPayload())
                .status(order.getStatus().name())
                .expiresAt(order.getExpiresAt())
                .confirmedAt(order.getConfirmedAt())
                .externalTransactionId(order.getExternalTransactionId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private BreBPaymentOrder toDomain(BreBPaymentOrderEntity entity) {
        return BreBPaymentOrder.reconstitute(
                entity.getId(),
                entity.getOrderCode(),
                entity.getProviderId(),
                Money.of(entity.getAmount()),
                entity.getBreBKey(),
                entity.getQrPayload(),
                BreBOrderStatus.valueOf(entity.getStatus()),
                entity.getExpiresAt(),
                entity.getConfirmedAt(),
                entity.getExternalTransactionId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
