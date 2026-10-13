package com.findu.security.presentation.controller;

import com.findu.security.application.usecase.ApproveProviderUseCase;
import com.findu.security.application.usecase.GetActivationStatusUseCase;
import com.findu.security.application.usecase.RejectProviderUseCase;
import com.findu.security.application.usecase.SubmitBiometricUseCase;
import com.findu.security.dto.request.ApproveProviderRequest;
import com.findu.security.dto.request.BiometricSubmitRequest;
import com.findu.security.dto.request.RejectProviderRequest;
import com.findu.security.dto.response.ActivationStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Activación del proveedor. Base {@code /api/v1/activation}. TODAS las rutas son estáticas (el
 * resolver de autorización compara {@code operation.path} por igualdad exacta); por eso approve/reject
 * reciben el {@code userId} en el body, no en el path.
 */
@RestController
@RequestMapping("/api/v1/activation")
@Tag(name = "Provider Activation", description = "Máquina de estados de activación del proveedor")
public class ProviderActivationController {

    private final SubmitBiometricUseCase submitBiometricUseCase;
    private final GetActivationStatusUseCase getActivationStatusUseCase;
    private final ApproveProviderUseCase approveProviderUseCase;
    private final RejectProviderUseCase rejectProviderUseCase;

    public ProviderActivationController(SubmitBiometricUseCase submitBiometricUseCase,
                                        GetActivationStatusUseCase getActivationStatusUseCase,
                                        ApproveProviderUseCase approveProviderUseCase,
                                        RejectProviderUseCase rejectProviderUseCase) {
        this.submitBiometricUseCase = submitBiometricUseCase;
        this.getActivationStatusUseCase = getActivationStatusUseCase;
        this.approveProviderUseCase = approveProviderUseCase;
        this.rejectProviderUseCase = rejectProviderUseCase;
    }

    @PostMapping("/biometric")
    @Operation(summary = "Enviar validación biométrica", description = "Re-valida nombre y fecha contra core; rechazos de negocio -> 200 con motivo")
    public Mono<ActivationStatusResponse> submitBiometric(@RequestBody @Valid BiometricSubmitRequest request) {
        return submitBiometricUseCase.submit(request);
    }

    @GetMapping("/status")
    @Operation(summary = "Estado de activación", description = "Estado + flags del proveedor autenticado")
    public Mono<ActivationStatusResponse> status() {
        return getActivationStatusUseCase.getStatus();
    }

    @PostMapping("/approve")
    @Operation(summary = "Aprobar proveedor (soporte)", description = "Precondición EN_REVISION_SOPORTE; userId en el body")
    public Mono<ActivationStatusResponse> approve(@RequestBody @Valid ApproveProviderRequest request) {
        return approveProviderUseCase.approve(request.userId());
    }

    @PostMapping("/reject")
    @Operation(summary = "Rechazar proveedor (soporte)", description = "Precondición EN_REVISION_SOPORTE; userId + motivo en el body")
    public Mono<ActivationStatusResponse> reject(@RequestBody @Valid RejectProviderRequest request) {
        return rejectProviderUseCase.reject(request.userId(), request.motivo());
    }
}
