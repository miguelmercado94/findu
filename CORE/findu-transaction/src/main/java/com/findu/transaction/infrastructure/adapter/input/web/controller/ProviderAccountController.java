package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.GetFinancialQueryUseCase;
import com.findu.transaction.application.port.in.GetProviderBalanceUseCase;
import com.findu.transaction.application.port.in.GetProviderLedgerUseCase;
import com.findu.transaction.application.port.in.query.GetProviderBalanceQuery;
import com.findu.transaction.application.port.in.query.GetProviderLedgerQuery;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.ProviderAccountResponse;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.ProviderLedgerEntryResponse;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.TransactionWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ProviderAccountController {

    private final GetFinancialQueryUseCase getFinancialQueryUseCase;
    private final GetProviderBalanceUseCase getProviderBalanceUseCase;
    private final GetProviderLedgerUseCase getProviderLedgerUseCase;
    private final TransactionWebMapper webMapper;

    @GetMapping({"/api/v1/providers/{providerId}/account", "/api/v1/provider-accounts/provider/{providerId}"})
    public ResponseEntity<ProviderAccountResponse> getProviderAccount(@PathVariable Long providerId) {
        GetProviderBalanceQuery query = GetProviderBalanceQuery.builder().providerId(providerId).build();
        return getProviderBalanceUseCase.execute(query)
                .or(() -> getFinancialQueryUseCase.getProviderAccount(providerId))
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping({"/api/v1/providers/{providerId}/ledger", "/api/v1/provider-accounts/provider/{providerId}/ledger"})
    public ResponseEntity<List<ProviderLedgerEntryResponse>> getProviderLedger(@PathVariable Long providerId) {
        GetProviderLedgerQuery query = GetProviderLedgerQuery.builder().providerId(providerId).build();
        List<ProviderLedgerEntry> entries = getProviderLedgerUseCase.execute(query);
        List<ProviderLedgerEntryResponse> response = entries.stream()
                .map(webMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
