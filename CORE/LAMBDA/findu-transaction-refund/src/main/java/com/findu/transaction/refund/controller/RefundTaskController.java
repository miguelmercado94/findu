package com.findu.transaction.refund.controller;

import com.findu.transaction.refund.service.RefundTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/refund")
@RequiredArgsConstructor
public class RefundTaskController {

    private final RefundTaskService refundTaskService;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runRefunds() {
        Map<String, Object> response = refundTaskService.runRefunds();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{refundId}/execute")
    public ResponseEntity<Object> executeRefund(
            @PathVariable String refundId,
            @RequestParam(required = false, defaultValue = "SUCCESS") String outcome) {
        Object response = refundTaskService.executeRefund(refundId, outcome);
        return ResponseEntity.ok(response);
    }
}
