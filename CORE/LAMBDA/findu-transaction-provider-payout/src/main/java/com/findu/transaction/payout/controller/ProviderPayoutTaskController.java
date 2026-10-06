package com.findu.transaction.payout.controller;

import com.findu.transaction.payout.service.ProviderPayoutTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/provider-payout")
@RequiredArgsConstructor
public class ProviderPayoutTaskController {

    private final ProviderPayoutTaskService providerPayoutTaskService;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runPayouts() {
        Map<String, Object> response = providerPayoutTaskService.runPayouts();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{payoutId}/execute")
    public ResponseEntity<Object> executePayout(
            @PathVariable String payoutId,
            @RequestParam(required = false, defaultValue = "SUCCESS") String outcome) {
        Object response = providerPayoutTaskService.executePayout(payoutId, outcome);
        return ResponseEntity.ok(response);
    }
}
