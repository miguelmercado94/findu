package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.CreatePayoutUseCase;
import com.findu.transaction.application.port.in.ExecutePayoutUseCase;
import com.findu.transaction.application.port.in.GetPayoutUseCase;
import com.findu.transaction.application.port.in.ManagePayoutUseCase;
import com.findu.transaction.application.port.in.command.CreatePayoutCommand;
import com.findu.transaction.application.port.in.command.ExecutePayoutCommand;
import com.findu.transaction.application.port.in.query.GetPayoutQuery;
import com.findu.transaction.domain.enums.TransferSimulationOutcome;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.CreatePayoutRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.PayoutResponse;
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
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final ManagePayoutUseCase managePayoutUseCase;
    private final CreatePayoutUseCase createPayoutUseCase;
    private final ExecutePayoutUseCase executePayoutUseCase;
    private final GetPayoutUseCase getPayoutUseCase;
    private final TransactionWebMapper webMapper;

    @PostMapping
    public ResponseEntity<PayoutResponse> createPayout(@Valid @RequestBody CreatePayoutRequest request) {
        log.info("REST: Creando solicitud de payout para providerId={}", request.getProviderId());
        CreatePayoutCommand command = webMapper.toCommand(request);
        Payout created = createPayoutUseCase != null ? createPayoutUseCase.execute(command) : managePayoutUseCase.createPayout(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(created));
    }

    @GetMapping("/{payoutId}")
    public ResponseEntity<PayoutResponse> getPayoutById(@PathVariable String payoutId) {
        GetPayoutQuery query = GetPayoutQuery.builder().payoutId(payoutId).build();
        return getPayoutUseCase.execute(query)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{payoutId}/execute")
    public ResponseEntity<PayoutResponse> executePayout(
            @PathVariable String payoutId,
            @RequestParam(required = false, defaultValue = "SUCCESS") TransferSimulationOutcome outcome) {
        log.info("REST: Ejecutando payout id={} con resultado={}", payoutId, outcome);
        ExecutePayoutCommand command = ExecutePayoutCommand.builder()
                .payoutId(payoutId)
                .outcome(outcome)
                .build();
        Payout executed = executePayoutUseCase != null ? executePayoutUseCase.execute(command) : managePayoutUseCase.executePayout(payoutId, outcome);
        return ResponseEntity.ok(webMapper.toResponse(executed));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PayoutResponse>> getPendingPayouts() {
        List<Payout> pending = getPayoutUseCase != null ? getPayoutUseCase.getPendingPayouts() : managePayoutUseCase.getPendingPayouts();
        return ResponseEntity.ok(webMapper.toPayoutResponseList(pending));
    }
}
