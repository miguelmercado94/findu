package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class GetDailyBalanceQuery {
    private final Long providerId;
    private final LocalDate date;
}
