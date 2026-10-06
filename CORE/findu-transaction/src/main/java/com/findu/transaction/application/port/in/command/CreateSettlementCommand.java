package com.findu.transaction.application.port.in.command;

import com.findu.transaction.domain.enums.SettlementType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CreateSettlementCommand {
    private final Long providerId;
    private final LocalDate targetDate;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final SettlementType settlementType;
}
