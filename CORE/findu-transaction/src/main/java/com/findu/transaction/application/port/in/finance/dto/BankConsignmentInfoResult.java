package com.findu.transaction.application.port.in.finance.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BankConsignmentInfoResult {
    private final String bankName;
    private final String accountType;
    private final String accountNumber;
    private final String maskedAccountNumber;
    private final String accountHolder;
    private final String nit;
    private final String paymentReference;
    private final String instructions;
}
