package com.findu.transaction.dailybalance.controller;

import com.findu.transaction.dailybalance.model.DailyBalanceExecutionResult;
import com.findu.transaction.dailybalance.service.DailyBalanceTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/internal/daily-balance")
@RequiredArgsConstructor
public class DailyBalanceTaskController {

    private final DailyBalanceTaskService dailyBalanceTaskService;

    @PostMapping("/run")
    public ResponseEntity<DailyBalanceExecutionResult> runDailyBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long providerId) {

        DailyBalanceExecutionResult result = dailyBalanceTaskService.runDailyBalance(date, providerId);
        return ResponseEntity.ok(result);
    }
}
