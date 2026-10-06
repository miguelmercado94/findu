package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.CreateRefundUseCase;
import com.findu.transaction.application.port.in.ExecuteRefundUseCase;
import com.findu.transaction.application.port.in.GetRefundUseCase;
import com.findu.transaction.application.port.in.ManageRefundUseCase;
import com.findu.transaction.application.port.in.command.CreateRefundCommand;
import com.findu.transaction.application.port.in.command.ExecuteRefundCommand;
import com.findu.transaction.application.port.in.query.GetRefundQuery;
import com.findu.transaction.domain.enums.TransferSimulationOutcome;
import com.findu.transaction.domain.model.refund.Refund;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.CreateRefundRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.RefundResponse;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.TransactionWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final ManageRefundUseCase manageRefundUseCase;
    private final CreateRefundUseCase createRefundUseCase;
    private final ExecuteRefundUseCase executeRefundUseCase;
    private final GetRefundUseCase getRefundUseCase;
    private final TransactionWebMapper webMapper;

    @PostMapping
    public ResponseEntity<RefundResponse> createRefund(@Valid @RequestBody CreateRefundRequest request) {
        log.info("REST: Creando solicitud de reembolso para transactionId={}", request.getTransactionId());
        CreateRefundCommand command = webMapper.toCommand(request);
        Refund created = createRefundUseCase != null ? createRefundUseCase.execute(command) : manageRefundUseCase.createRefund(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(created));
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponse> getRefundById(@PathVariable String refundId) {
        GetRefundQuery query = GetRefundQuery.builder().refundId(refundId).build();
        return getRefundUseCase.execute(query)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{refundId}/execute")
    public ResponseEntity<RefundResponse> executeRefund(
            @PathVariable String refundId,
            @RequestParam(required = false, defaultValue = "SUCCESS") TransferSimulationOutcome outcome) {
        log.info("REST: Ejecutando reembolso id={} con resultado={}", refundId, outcome);
        ExecuteRefundCommand command = ExecuteRefundCommand.builder()
                .refundId(refundId)
                .outcome(outcome)
                .build();
        Refund executed = executeRefundUseCase != null ? executeRefundUseCase.execute(command) : manageRefundUseCase.executeRefund(refundId, outcome);
        return ResponseEntity.ok(webMapper.toResponse(executed));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<RefundResponse>> getPendingRefunds() {
        List<Refund> pending = getRefundUseCase != null ? getRefundUseCase.getPendingRefunds() : manageRefundUseCase.getPendingRefunds();
        return ResponseEntity.ok(webMapper.toRefundResponseList(pending));
    }
}
