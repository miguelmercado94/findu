package com.findu.transaction.application.port.in.command;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CalculateDailyBalanceCommand {
    private final Long providerId;
    private final LocalDate date;
}
