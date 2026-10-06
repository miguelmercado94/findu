package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.CalculateDailyBalanceUseCase;
import com.findu.transaction.application.port.in.GetDailyBalanceUseCase;
import com.findu.transaction.application.port.in.ManageDailyBalanceUseCase;
import com.findu.transaction.application.port.in.command.CalculateDailyBalanceCommand;
import com.findu.transaction.application.port.in.query.GetDailyBalanceQuery;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.DailyBalanceResponse;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.TransactionWebMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DailyBalanceController {

    private final ManageDailyBalanceUseCase manageDailyBalanceUseCase;
    private final CalculateDailyBalanceUseCase calculateDailyBalanceUseCase;
    private final GetDailyBalanceUseCase getDailyBalanceUseCase;
    private final TransactionWebMapper webMapper;

    @PostMapping("/api/v1/daily-balances/calculate")
    public ResponseEntity<DailyBalanceResponse> calculateDailyBalance(
            @RequestParam Long providerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate balanceDate) {

        log.info("REST: Calculando balance diario para providerId={} y fecha={}", providerId, balanceDate);
        CalculateDailyBalanceCommand command = CalculateDailyBalanceCommand.builder()
                .providerId(providerId)
                .date(balanceDate)
                .build();
        DailyBalance balance = calculateDailyBalanceUseCase != null 
                ? calculateDailyBalanceUseCase.calculateForProvider(command) 
                : manageDailyBalanceUseCase.calculateDailyBalance(providerId, balanceDate);
        return ResponseEntity.ok(webMapper.toResponse(balance));
    }

    @PostMapping("/api/v1/daily-balances/calculate-all")
    public ResponseEntity<List<DailyBalanceResponse>> calculateAllDailyBalances(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate balanceDate) {

        log.info("REST: Calculando todos los balances diarios para fecha={}", balanceDate);
        List<DailyBalance> list = calculateDailyBalanceUseCase != null 
                ? calculateDailyBalanceUseCase.calculateForAllProviders(balanceDate)
                : manageDailyBalanceUseCase.calculateAllDailyBalances(balanceDate);
        return ResponseEntity.ok(webMapper.toDailyBalanceResponseList(list));
    }

    @GetMapping({"/api/v1/providers/{providerId}/daily-balances", "/api/v1/daily-balances/provider/{providerId}"})
    public ResponseEntity<DailyBalanceResponse> getDailyBalance(
            @PathVariable Long providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate balanceDate) {

        LocalDate date = balanceDate != null ? balanceDate : LocalDate.now();
        GetDailyBalanceQuery query = GetDailyBalanceQuery.builder()
                .providerId(providerId)
                .date(date)
                .build();

        return getDailyBalanceUseCase.execute(query)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
