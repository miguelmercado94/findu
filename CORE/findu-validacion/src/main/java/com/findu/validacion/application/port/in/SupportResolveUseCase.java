package com.findu.validacion.application.port.in;

import com.findu.validacion.application.dto.request.SupportResolveRequest;
import com.findu.validacion.application.dto.response.SupportResolveResponse;
import reactor.core.publisher.Mono;

public interface SupportResolveUseCase {
    Mono<SupportResolveResponse> resolve(SupportResolveRequest request);
}
