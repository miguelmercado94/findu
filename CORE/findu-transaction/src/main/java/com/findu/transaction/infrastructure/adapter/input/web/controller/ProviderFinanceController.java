package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.finance.*;
import com.findu.transaction.application.port.in.finance.command.ConfirmBankConsignmentCommand;
import com.findu.transaction.application.port.in.finance.command.CreateBreBOrderCommand;
import com.findu.transaction.application.port.in.finance.dto.*;
import com.findu.transaction.domain.model.finance.BankConsignmentReport;
import com.findu.transaction.domain.model.finance.BreBPaymentOrder;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.ConfirmBankConsignmentRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.CreateBreBOrderRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.*;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.FinanceWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ProviderFinanceController {

    private final GetWeeklyBalanceUseCase getWeeklyBalanceUseCase;
    private final GetDebtStatusUseCase getDebtStatusUseCase;
    private final GetFinancialMovementsUseCase getFinancialMovementsUseCase;
    private final GetBankConsignmentInfoUseCase getBankConsignmentInfoUseCase;
    private final CreateBreBOrderUseCase createBreBOrderUseCase;
    private final GetBreBOrderUseCase getBreBOrderUseCase;
    private final ConfirmBankConsignmentUseCase confirmBankConsignmentUseCase;
    private final FinanceWebMapper mapper;

    /**
     * Pantalla Principal de Finanzas: Balance provisional/negativo, corte domingo y liquidación martes.
     */
    @GetMapping("/api/v1/providers/{providerId}/finances/weekly-balance")
    public ResponseEntity<WeeklyBalanceResponse> getWeeklyBalance(
            @PathVariable Long providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String username) {

        log.info("REST: Consultando balance semanal para providerId={}, date={}", providerId, date);
        WeeklyBalanceResult result = getWeeklyBalanceUseCase.getWeeklyBalance(providerId, date, username);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    /**
     * Límite de saldo negativo y estado de restricción.
     */
    @GetMapping("/api/v1/providers/{providerId}/finances/debt-status")
    public ResponseEntity<DebtStatusResponse> getDebtStatus(@PathVariable Long providerId) {
        log.info("REST: Consultando estado de deuda para providerId={}", providerId);
        DebtStatusResult result = getDebtStatusUseCase.getDebtStatus(providerId);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    /**
     * Historial de movimientos filtrado por: THIS_WEEK, LAST_30_DAYS, ALL.
     */
    @GetMapping("/api/v1/providers/{providerId}/finances/movements")
    public ResponseEntity<FinancialMovementsResponse> getMovements(
            @PathVariable Long providerId,
            @RequestParam(defaultValue = "THIS_WEEK") String filter) {

        log.info("REST: Consultando movimientos para providerId={}, filter={}", providerId, filter);
        FinancialMovementsResult result = getFinancialMovementsUseCase.getMovements(providerId, filter);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    /**
     * Información de cuenta empresarial para consignación bancaria y referencia nameuser-{serial}.
     */
    @GetMapping("/api/v1/providers/{providerId}/finances/bank-consignment-info")
    public ResponseEntity<BankConsignmentInfoResponse> getBankConsignmentInfo(
            @PathVariable Long providerId,
            @RequestParam(required = false) String username) {

        log.info("REST: Consultando datos de consignación bancaria para providerId={}", providerId);
        BankConsignmentInfoResult result = getBankConsignmentInfoUseCase.getBankConsignmentInfo(providerId, username);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    /**
     * Crea una orden de pago Bre-B con llave personalizada y QR dinámico (vigente por 10 min).
     */
    @PostMapping("/api/v1/providers/{providerId}/finances/bre-b/orders")
    public ResponseEntity<BreBOrderResponse> createBreBOrder(
            @PathVariable Long providerId,
            @Valid @RequestBody CreateBreBOrderRequest request) {

        log.info("REST: Creando orden de pago Bre-B para providerId={}, amount={}", providerId, request.getAmount());
        CreateBreBOrderCommand command = mapper.toCommand(providerId, request);
        BreBPaymentOrder order = createBreBOrderUseCase.createOrder(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(order));
    }

    /**
     * Consulta el estado de una orden Bre-B (polling o verificación).
     */
    @GetMapping({"/api/v1/finances/bre-b/orders/{orderId}", "/api/v1/providers/{providerId}/finances/bre-b/orders/{orderId}"})
    public ResponseEntity<BreBOrderResponse> getBreBOrder(
            @PathVariable(required = false) Long providerId,
            @PathVariable String orderId) {

        return getBreBOrderUseCase.getOrderById(orderId)
                .or(() -> getBreBOrderUseCase.getOrderByCode(orderId))
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Conciliación bancaria interna / administrativa para confirmar abonos.
     */
    @PostMapping("/api/v1/finances/bank-consignments/confirm")
    public ResponseEntity<Map<String, Object>> confirmBankConsignment(
            @Valid @RequestBody ConfirmBankConsignmentRequest request) {

        log.info("REST: Confirmando consignación bancaria ref={}, providerId={}", request.getReference(), request.getProviderId());
        ConfirmBankConsignmentCommand command = mapper.toCommand(request);
        BankConsignmentReport report = confirmBankConsignmentUseCase.confirmConsignment(command);

        return ResponseEntity.ok(Map.of(
                "status", "CONFIRMED",
                "reportId", report.getId(),
                "reference", report.getReference(),
                "amount", report.getAmount().getAmount(),
                "confirmedAt", report.getConfirmedAt()
        ));
    }
}
