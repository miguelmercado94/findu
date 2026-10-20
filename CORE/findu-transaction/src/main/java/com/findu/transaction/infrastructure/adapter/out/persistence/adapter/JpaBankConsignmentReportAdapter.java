package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.BankConsignmentReportRepository;
import com.findu.transaction.domain.enums.PaymentStatus;
import com.findu.transaction.domain.model.finance.BankConsignmentReport;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.BankConsignmentReportEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataBankConsignmentReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaBankConsignmentReportAdapter implements BankConsignmentReportRepository {

    private final SpringDataBankConsignmentReportRepository jpaRepository;

    @Override
    public BankConsignmentReport save(BankConsignmentReport report) {
        BankConsignmentReportEntity entity = toEntity(report);
        BankConsignmentReportEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<BankConsignmentReport> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<BankConsignmentReport> findByReference(String reference) {
        return jpaRepository.findByReference(reference).map(this::toDomain);
    }

    @Override
    public List<BankConsignmentReport> findByProviderId(Long providerId) {
        return jpaRepository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private BankConsignmentReportEntity toEntity(BankConsignmentReport report) {
        return BankConsignmentReportEntity.builder()
                .id(report.getId())
                .providerId(report.getProviderId())
                .amount(report.getAmount().getAmount())
                .reference(report.getReference())
                .bankName(report.getBankName())
                .status(report.getStatus().name())
                .confirmedAt(report.getConfirmedAt())
                .confirmedBy(report.getConfirmedBy())
                .notes(report.getNotes())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private BankConsignmentReport toDomain(BankConsignmentReportEntity entity) {
        return BankConsignmentReport.reconstitute(
                entity.getId(),
                entity.getProviderId(),
                Money.of(entity.getAmount()),
                entity.getReference(),
                entity.getBankName(),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getConfirmedAt(),
                entity.getConfirmedBy(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
