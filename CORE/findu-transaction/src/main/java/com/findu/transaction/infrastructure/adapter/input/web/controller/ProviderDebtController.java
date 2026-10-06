package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.GetProviderDebtUseCase;
import com.findu.transaction.application.port.in.ManageProviderDebtUseCase;
import com.findu.transaction.application.port.in.PayProviderDebtUseCase;
import com.findu.transaction.application.port.in.command.PayProviderDebtCommand;
import com.findu.transaction.application.port.in.query.GetProviderDebtQuery;
import com.findu.transaction.domain.model.debt.ProviderDebtPayment;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.PayDebtRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.ProviderDebtPaymentResponse;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.TransactionWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ProviderDebtController {

    private final ManageProviderDebtUseCase manageProviderDebtUseCase;
    private final PayProviderDebtUseCase payProviderDebtUseCase;
    private final GetProviderDebtUseCase getProviderDebtUseCase;
    private final TransactionWebMapper webMapper;

    @GetMapping({"/api/v1/providers/{providerId}/debt", "/api/v1/provider-debts/provider/{providerId}"})
    public ResponseEntity<Map<String, Object>> getProviderDebt(@PathVariable Long providerId) {
        GetProviderDebtQuery query = GetProviderDebtQuery.builder().providerId(providerId).build();
        Money debt = getProviderDebtUseCase.execute(query);
        return ResponseEntity.ok(Map.of(
                "providerId", providerId,
                "debtAmount", debt.getAmount(),
                "currency", debt.getCurrency().getCurrencyCode()
        ));
    }

    @PostMapping({"/api/v1/provider-debts/pay", "/api/v1/providers/{providerId}/debt-payments"})
    public ResponseEntity<ProviderDebtPaymentResponse> payDebt(
            @PathVariable(required = false) Long providerId,
            @Valid @RequestBody PayDebtRequest request) {

        if (providerId != null) {
            request.setProviderId(providerId);
        }

        log.info("REST: Registrando pago de deuda para providerId={}", request.getProviderId());
        PayProviderDebtCommand command = webMapper.toCommand(request);
        ProviderDebtPayment payment = payProviderDebtUseCase != null ? payProviderDebtUseCase.execute(command) : manageProviderDebtUseCase.payDebt(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(payment));
    }
}
