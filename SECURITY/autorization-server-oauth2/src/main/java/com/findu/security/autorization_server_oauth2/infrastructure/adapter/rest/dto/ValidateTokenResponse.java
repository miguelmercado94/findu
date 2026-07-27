package com.findu.security.autorization_server_oauth2.infrastructure.adapter.rest.dto;

import java.util.Map;

public record ValidateTokenResponse(
    boolean tokenValid,
    Map<String, Object> header,
    Map<String, Object> payload
) {}
