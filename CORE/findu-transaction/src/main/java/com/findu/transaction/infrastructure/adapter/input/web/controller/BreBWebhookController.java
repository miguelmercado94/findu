package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.transaction.application.port.in.finance.ProcessBreBWebhookUseCase;
import com.findu.transaction.application.port.in.finance.command.ProcessBreBWebhookCommand;
import com.findu.transaction.domain.model.finance.BreBPaymentOrder;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.BreBWebhookPayload;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.BreBOrderResponse;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.FinanceWebMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BreBWebhookController {

    private final ProcessBreBWebhookUseCase processBreBWebhookUseCase;
    private final FinanceWebMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * Webhook oficial para confirmaciones del switch Bre-B o del banco.
     */
    @PostMapping("/api/v1/finances/bre-b/webhook")
    public ResponseEntity<BreBOrderResponse> handleBreBWebhook(
            @RequestHeader(value = "X-BreB-Signature", required = false) String signature,
            @RequestBody String rawPayload) {

        log.info("REST: Webhook Bre-B recibido con signature={}", signature);

        try {
            BreBWebhookPayload payload = objectMapper.readValue(rawPayload, BreBWebhookPayload.class);
            ProcessBreBWebhookCommand command = mapper.toCommand(payload, signature, rawPayload);
            BreBPaymentOrder order = processBreBWebhookUseCase.processWebhook(command);
            return ResponseEntity.ok(mapper.toResponse(order));
        } catch (SecurityException e) {
            log.warn("Firma no autorizada en webhook Bre-B: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            log.error("Error procesando webhook Bre-B: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
