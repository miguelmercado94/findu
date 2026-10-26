package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.request.RegisterBrebKeyRequest;
import com.findu.validacion.application.dto.request.UpdateBrebKeyRequest;
import com.findu.validacion.application.dto.response.ProviderBrebKeyResponse;
import com.findu.validacion.application.port.in.ManageProviderBrebKeyUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/validations/bre-b-keys")
@RequiredArgsConstructor
@Tag(name = "Provider Bre-B Keys", description = "Gestión de llaves Bre-B para proveedores con validación de seguridad OTP")
public class ProviderBrebKeyController {

    private final ManageProviderBrebKeyUseCase manageBrebKeyUseCase;

    @Operation(summary = "Vincular llave Bre-B",
            description = "Registra una nueva llave Bre-B para el proveedor validando el código OTP por celular.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<ProviderBrebKeyResponse>> registerKey(
            @Valid @RequestBody RegisterBrebKeyRequest request) {
        return manageBrebKeyUseCase.registerKey(request)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res));
    }

    @Operation(summary = "Actualizar llave Bre-B",
            description = "Actualiza la llave Bre-B del proveedor validando el código OTP por celular.")
    @PutMapping("/{providerId}")
    public Mono<ResponseEntity<ProviderBrebKeyResponse>> updateKey(
            @PathVariable Long providerId,
            @Valid @RequestBody UpdateBrebKeyRequest request) {
        return manageBrebKeyUseCase.updateKey(providerId, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar llave Bre-B activa",
            description = "Retorna la llave Bre-B activa del proveedor con enmascaramiento seguro.")
    @GetMapping("/{providerId}")
    public Mono<ResponseEntity<ProviderBrebKeyResponse>> getKey(
            @PathVariable Long providerId) {
        return manageBrebKeyUseCase.getKey(providerId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Desactivar llave Bre-B",
            description = "Marca como inactiva la llave Bre-B del proveedor.")
    @DeleteMapping("/{providerId}")
    public Mono<ResponseEntity<Void>> deactivateKey(
            @PathVariable Long providerId) {
        return manageBrebKeyUseCase.deactivateKey(providerId)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
