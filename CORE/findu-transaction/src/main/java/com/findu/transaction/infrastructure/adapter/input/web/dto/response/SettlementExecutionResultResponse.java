package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SettlementExecutionResultResponse {
    private String executionId;
    private LocalDate targetDate;
    private int providersProcessed;
    private int settlementsCreated;
    private int settlementsSkipped;
    private int providersFailed;
    private Instant startedAt;
    private Instant finishedAt;
    private String status;
    private List<String> errors;
}
