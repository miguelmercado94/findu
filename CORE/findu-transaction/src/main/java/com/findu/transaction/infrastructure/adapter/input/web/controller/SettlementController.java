package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.CreateSettlementUseCase;
import com.findu.transaction.application.port.in.GetSettlementUseCase;
import com.findu.transaction.application.port.in.command.CreateSettlementCommand;
import com.findu.transaction.application.port.in.query.GetSettlementQuery;
import com.findu.transaction.application.service.ManageSettlementService;
import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.CreateSettlementRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.SettlementExecutionResultResponse;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.SettlementResponse;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.TransactionWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class SettlementController {

    private final CreateSettlementUseCase createSettlementUseCase;
    private final GetSettlementUseCase getSettlementUseCase;
    private final ManageSettlementService manageSettlementService;
    private final TransactionWebMapper webMapper;

    @PostMapping("/internal/settlements/run")
    public ResponseEntity<SettlementExecutionResultResponse> runBatchSettlements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String type) {
        SettlementExecutionResultResponse result = manageSettlementService.runBatchSettlements(date, type);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/internal/settlements/provider/{providerId}")
    public ResponseEntity<SettlementResponse> runProviderSettlement(
            @PathVariable Long providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) SettlementType type) {

        CreateSettlementCommand command = CreateSettlementCommand.builder()
                .providerId(providerId)
                .targetDate(date)
                .settlementType(type)
                .build();

        Settlement created = createSettlementUseCase.execute(command);
        return ResponseEntity.ok(webMapper.toResponse(created));
    }

    @PostMapping("/api/v1/settlements")
    public ResponseEntity<SettlementResponse> createSettlement(@Valid @RequestBody CreateSettlementRequest request) {
        CreateSettlementCommand command = webMapper.toCommand(request);
        Settlement created = createSettlementUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(created));
    }

    @GetMapping("/api/v1/settlements/{settlementId}")
    public ResponseEntity<SettlementResponse> getSettlementById(@PathVariable String settlementId) {
        GetSettlementQuery query = GetSettlementQuery.builder()
                .settlementId(settlementId)
                .build();
        return getSettlementUseCase.execute(query)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/providers/{providerId}/settlements")
    public ResponseEntity<SettlementResponse> getSettlementByProviderId(@PathVariable Long providerId) {
        GetSettlementQuery query = GetSettlementQuery.builder()
                .providerId(providerId)
                .build();
        return getSettlementUseCase.execute(query)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
