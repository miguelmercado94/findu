package com.findu.transaction.domain.model.settlement;

import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class ProviderSettlementProfile {

    private String id;
    private Long providerId;
    private ProviderType providerType;
    private SettlementType settlementFrequency;
    private String settlementDay;
    private String settlementTime;
    private Money negativeBalanceLimit;
    private Instant createdAt;
    private Instant updatedAt;

    private ProviderSettlementProfile() {}

    public static ProviderSettlementProfile createDefault(Long providerId, ProviderType providerType) {
        if (providerId == null) throw new IllegalArgumentException("El providerId es obligatorio");
        ProviderType type = providerType != null ? providerType : ProviderType.PERSONA_NATURAL;

        ProviderSettlementProfile profile = new ProviderSettlementProfile();
        profile.id = UUID.randomUUID().toString();
        profile.providerId = providerId;
        profile.providerType = type;

        if (type == ProviderType.CORPORATIVO) {
            profile.settlementFrequency = SettlementType.WEEKLY;
            profile.settlementDay = "FRIDAY";
            profile.settlementTime = "14:00";
            profile.negativeBalanceLimit = Money.of(new BigDecimal("500000.00"));
        } else {
            profile.settlementFrequency = SettlementType.DAILY;
            profile.settlementDay = "EVERYDAY";
            profile.settlementTime = "06:00";
            profile.negativeBalanceLimit = Money.of(new BigDecimal("100000.00"));
        }

        profile.createdAt = Instant.now();
        profile.updatedAt = Instant.now();
        return profile;
    }

    public static ProviderSettlementProfile reconstitute(
            String id,
            Long providerId,
            ProviderType providerType,
            SettlementType settlementFrequency,
            String settlementDay,
            String settlementTime,
            Money negativeBalanceLimit,
            Instant createdAt,
            Instant updatedAt) {

        ProviderSettlementProfile profile = new ProviderSettlementProfile();
        profile.id = id;
        profile.providerId = providerId;
        profile.providerType = providerType;
        profile.settlementFrequency = settlementFrequency;
        profile.settlementDay = settlementDay;
        profile.settlementTime = settlementTime;
        profile.negativeBalanceLimit = negativeBalanceLimit;
        profile.createdAt = createdAt;
        profile.updatedAt = updatedAt;
        return profile;
    }
}
