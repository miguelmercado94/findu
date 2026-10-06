package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.CreateServiceTransactionUseCase;
import com.findu.transaction.application.port.in.GetCustomerTransactionsUseCase;
import com.findu.transaction.application.port.in.GetFinancialQueryUseCase;
import com.findu.transaction.application.port.in.RegisterPaymentUseCase;
import com.findu.transaction.application.port.in.command.CreateServiceTransactionCommand;
import com.findu.transaction.application.port.in.command.RegisterPaymentCommand;
import com.findu.transaction.application.port.in.query.GetCustomerTransactionsQuery;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.CreatePaymentRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.CreateTransactionRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.ServiceTransactionResponse;
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
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class ServiceTransactionController {

    private final CreateServiceTransactionUseCase createServiceTransactionUseCase;
    private final GetFinancialQueryUseCase getFinancialQueryUseCase;
    private final GetCustomerTransactionsUseCase getCustomerTransactionsUseCase;
    private final RegisterPaymentUseCase registerPaymentUseCase;
    private final TransactionWebMapper webMapper;

    @PostMapping
    public ResponseEntity<ServiceTransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        log.info("REST: Creando transacción de servicio para serviceRequestId={}", request.getServiceRequestId());
        CreateServiceTransactionCommand command = webMapper.toCommand(request);
        ServiceTransaction created = createServiceTransactionUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceTransactionResponse> getTransactionById(@PathVariable String id) {
        return getFinancialQueryUseCase.getTransactionById(id)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{transactionCode}")
    public ResponseEntity<ServiceTransactionResponse> getTransactionByCode(@PathVariable String transactionCode) {
        return getFinancialQueryUseCase.getTransactionByCode(transactionCode)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ServiceTransactionResponse>> queryTransactions(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long providerId) {

        if (customerId != null) {
            List<ServiceTransaction> list = getCustomerTransactionsUseCase.execute(
                    GetCustomerTransactionsQuery.builder().customerId(customerId).build()
            );
            return ResponseEntity.ok(webMapper.toTransactionResponseList(list));
        }

        if (providerId != null) {
            List<ServiceTransaction> list = getFinancialQueryUseCase.getTransactionsByProviderId(providerId);
            return ResponseEntity.ok(webMapper.toTransactionResponseList(list));
        }

        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ServiceTransactionResponse>> getTransactionsByProviderId(@PathVariable Long providerId) {
        List<ServiceTransaction> list = getFinancialQueryUseCase.getTransactionsByProviderId(providerId);
        return ResponseEntity.ok(webMapper.toTransactionResponseList(list));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ServiceTransactionResponse>> getTransactionsByCustomerId(@PathVariable Long customerId) {
        List<ServiceTransaction> list = getCustomerTransactionsUseCase.execute(
                GetCustomerTransactionsQuery.builder().customerId(customerId).build()
        );
        return ResponseEntity.ok(webMapper.toTransactionResponseList(list));
    }

    @PostMapping("/{transactionId}/payments")
    public ResponseEntity<ServiceTransactionResponse> registerPayment(
            @PathVariable String transactionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        log.info("REST: Registrando pago para transactionId={}, idempotencyKey={}", transactionId, idempotencyKey);
        RegisterPaymentCommand command = webMapper.toCommand(transactionId, request);
        ServiceTransaction updatedTx = registerPaymentUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(updatedTx));
    }

    @GetMapping("/{transactionId}/payments")
    public ResponseEntity<ServiceTransactionResponse> getPaymentDetails(@PathVariable String transactionId) {
        return getFinancialQueryUseCase.getTransactionById(transactionId)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
