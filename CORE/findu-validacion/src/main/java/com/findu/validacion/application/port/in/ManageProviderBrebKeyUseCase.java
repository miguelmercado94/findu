package com.findu.validacion.application.port.in;

import com.findu.validacion.application.dto.request.RegisterBrebKeyRequest;
import com.findu.validacion.application.dto.request.UpdateBrebKeyRequest;
import com.findu.validacion.application.dto.response.ProviderBrebKeyResponse;
import reactor.core.publisher.Mono;

public interface ManageProviderBrebKeyUseCase {
    Mono<ProviderBrebKeyResponse> registerKey(RegisterBrebKeyRequest request);
    Mono<ProviderBrebKeyResponse> updateKey(Long providerId, UpdateBrebKeyRequest request);
    Mono<ProviderBrebKeyResponse> getKey(Long providerId);
    Mono<Void> deactivateKey(Long providerId);
}
