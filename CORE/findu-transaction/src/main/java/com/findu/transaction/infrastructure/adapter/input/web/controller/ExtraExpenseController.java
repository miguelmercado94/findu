package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.ApproveExtraExpenseUseCase;
import com.findu.transaction.application.port.in.GetExtraExpenseUseCase;
import com.findu.transaction.application.port.in.ManageExtraExpenseUseCase;
import com.findu.transaction.application.port.in.RejectExtraExpenseUseCase;
import com.findu.transaction.application.port.in.command.ApproveExtraExpenseCommand;
import com.findu.transaction.application.port.in.command.RejectExtraExpenseCommand;
import com.findu.transaction.application.port.in.command.RequestExtraExpenseCommand;
import com.findu.transaction.application.port.in.query.GetExtraExpenseQuery;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.RequestExtraExpenseRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.ExtraExpenseResponse;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.TransactionWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExtraExpenseController {

    private final ManageExtraExpenseUseCase manageExtraExpenseUseCase;
    private final GetExtraExpenseUseCase getExtraExpenseUseCase;
    private final ApproveExtraExpenseUseCase approveExtraExpenseUseCase;
    private final RejectExtraExpenseUseCase rejectExtraExpenseUseCase;
    private final TransactionWebMapper webMapper;

    @PostMapping("/api/v1/extra-expenses")
    public ResponseEntity<ExtraExpenseResponse> requestExpense(@Valid @RequestBody RequestExtraExpenseRequest request) {
        log.info("REST: Solicitando gasto adicional para serviceRequestId={}", request.getServiceRequestId());
        RequestExtraExpenseCommand command = webMapper.toCommand(request);
        ExtraExpenseRequest created = manageExtraExpenseUseCase.requestExpense(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(created));
    }

    @PostMapping("/api/v1/transactions/{transactionId}/extra-expenses")
    public ResponseEntity<ExtraExpenseResponse> requestExpenseForTransaction(
            @PathVariable String transactionId,
            @Valid @RequestBody RequestExtraExpenseRequest request) {
        log.info("REST: Solicitando gasto adicional en transacción id={}", transactionId);
        RequestExtraExpenseCommand command = webMapper.toCommand(request);
        ExtraExpenseRequest created = manageExtraExpenseUseCase.requestExpense(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(created));
    }

    @GetMapping("/api/v1/extra-expenses/{expenseId}")
    public ResponseEntity<ExtraExpenseResponse> getExtraExpenseById(@PathVariable String expenseId) {
        GetExtraExpenseQuery query = GetExtraExpenseQuery.builder()
                .expenseId(expenseId)
                .build();
        return getExtraExpenseUseCase.execute(query)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/v1/extra-expenses/{expenseId}/approve")
    public ResponseEntity<ExtraExpenseResponse> approveExpensePost(@PathVariable String expenseId) {
        log.info("REST: Aprobando gasto adicional id={}", expenseId);
        ExtraExpenseRequest approved = approveExtraExpenseUseCase.execute(
                ApproveExtraExpenseCommand.builder().expenseId(expenseId).build()
        );
        return ResponseEntity.ok(webMapper.toResponse(approved));
    }

    @PutMapping("/api/v1/extra-expenses/{expenseId}/approve")
    public ResponseEntity<ExtraExpenseResponse> approveExpensePut(@PathVariable String expenseId) {
        return approveExpensePost(expenseId);
    }

    @PostMapping("/api/v1/extra-expenses/{expenseId}/reject")
    public ResponseEntity<ExtraExpenseResponse> rejectExpensePost(@PathVariable String expenseId) {
        log.info("REST: Rechazando gasto adicional id={}", expenseId);
        ExtraExpenseRequest rejected = rejectExtraExpenseUseCase.execute(
                RejectExtraExpenseCommand.builder().expenseId(expenseId).build()
        );
        return ResponseEntity.ok(webMapper.toResponse(rejected));
    }

    @PutMapping("/api/v1/extra-expenses/{expenseId}/reject")
    public ResponseEntity<ExtraExpenseResponse> rejectExpensePut(@PathVariable String expenseId) {
        return rejectExpensePost(expenseId);
    }
}
