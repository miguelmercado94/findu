package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import com.findu.transaction.domain.enums.ProviderAccountStatus;
import com.findu.transaction.domain.enums.ProviderType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class ProviderAccountResponse {
    private String id;
    private Long providerId;
    private ProviderType providerType;
    private BigDecimal payableBalance;
    private BigDecimal receivableBalance;
    private BigDecimal debtLimit;
    private String currency;
    private ProviderAccountStatus status;
    private List<ProviderLedgerEntryResponse> ledgerEntries;
    private Instant createdAt;
    private Instant updatedAt;
}
