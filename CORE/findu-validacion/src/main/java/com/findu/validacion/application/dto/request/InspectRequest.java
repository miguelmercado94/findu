package com.findu.validacion.application.dto.request;

public record InspectRequest(
        String serviceName,
        String httpMethod,
        String path
) {
}
