package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankConsignmentInfoResponse {
    private String bankName;
    private String accountType;
    private String accountNumber;
    private String maskedAccountNumber;
    private String accountHolder;
    private String nit;
    private String paymentReference;
    private String instructions;
}
