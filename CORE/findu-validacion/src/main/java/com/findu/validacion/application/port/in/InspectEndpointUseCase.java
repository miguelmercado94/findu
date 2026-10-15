package com.findu.validacion.application.port.in;

import com.findu.validacion.application.dto.request.InspectRequest;
import com.findu.validacion.application.dto.response.InspectResponse;
import reactor.core.publisher.Mono;

public interface InspectEndpointUseCase {
    Mono<InspectResponse> inspect(InspectRequest request);
}
