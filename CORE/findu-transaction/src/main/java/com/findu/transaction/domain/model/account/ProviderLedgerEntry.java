package com.findu.transaction.domain.model.account;

import com.findu.transaction.domain.enums.LedgerDirection;
import com.findu.transaction.domain.enums.LedgerEntryType;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ProviderLedgerEntry {

    private final String entryId;
    private final Long providerId;
    private final LedgerEntryType type;
    private final Money amount;
    private final LedgerDirection direction;
    private final String referenceId;
    private final String description;
    private final Instant createdAt;

    public ProviderLedgerEntry(
            Long providerId,
            LedgerEntryType type,
            Money amount,
            LedgerDirection direction,
            String referenceId,
            String description) {

        if (providerId == null) throw new IllegalArgumentException("El providerId es obligatorio");
        if (type == null) throw new IllegalArgumentException("El tipo de movimiento ledger es obligatorio");
        if (amount == null) throw new IllegalArgumentException("El monto es obligatorio");
        if (direction == null) throw new IllegalArgumentException("La dirección del movimiento es obligatoria");

        this.entryId = UUID.randomUUID().toString();
        this.providerId = providerId;
        this.type = type;
        this.amount = amount;
        this.direction = direction;
        this.referenceId = referenceId != null ? referenceId : "";
        this.description = description != null ? description : "";
        this.createdAt = Instant.now();
    }

    public ProviderLedgerEntry(
            String entryId,
            Long providerId,
            LedgerEntryType type,
            Money amount,
            LedgerDirection direction,
            String referenceId,
            String description,
            Instant createdAt) {

        this.entryId = entryId;
        this.providerId = providerId;
        this.type = type;
        this.amount = amount;
        this.direction = direction;
        this.referenceId = referenceId;
        this.description = description;
        this.createdAt = createdAt;
    }
}
