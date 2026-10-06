package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import com.findu.transaction.domain.enums.LedgerDirection;
import com.findu.transaction.domain.enums.LedgerEntryType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ProviderLedgerEntryResponse {
    private String entryId;
    private Long providerId;
    private LedgerEntryType type;
    private BigDecimal amount;
    private String currency;
    private LedgerDirection direction;
    private String referenceId;
    private String description;
    private Instant createdAt;
}
