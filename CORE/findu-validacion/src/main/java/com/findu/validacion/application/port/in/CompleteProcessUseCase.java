package com.findu.validacion.application.port.in;

import com.findu.validacion.application.dto.request.CompleteProcessRequest;
import com.findu.validacion.application.dto.response.CompleteProcessResponse;
import reactor.core.publisher.Mono;

public interface CompleteProcessUseCase {
    Mono<CompleteProcessResponse> complete(CompleteProcessRequest request);
}
