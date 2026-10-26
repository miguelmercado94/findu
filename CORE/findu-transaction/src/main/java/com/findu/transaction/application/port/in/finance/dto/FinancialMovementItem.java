package com.findu.transaction.application.port.in.finance.dto;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FinancialMovementItem {
    private final String id;
    private final Long serviceRequestId;
    private final String serviceCode;
    private final String serviceName;
    private final String specializationName;
    private final String title;
    private final String subtitle;
    private final Instant date;
    private final String dateLabel;
    private final String timeLabel;
    private final Money amount;
    private final boolean isIncome; // true: positivo / verde, false: egreso / rojo
    private final String type;
    private final String status; // PROGRAMADO, RECIBIDO, COMPLETADO, etc.
    private final String reference;
}

