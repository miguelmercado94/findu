package com.findu.validacion.application.port.in;

import com.findu.validacion.application.dto.request.CheckActiveRequest;
import com.findu.validacion.application.dto.response.CheckActiveResponse;
import reactor.core.publisher.Mono;

public interface CheckActiveValidationsUseCase {
    Mono<CheckActiveResponse> check(CheckActiveRequest request);
}
