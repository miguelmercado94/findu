package com.findu.security.autorization_server_oauth2.infrastructure.adapter.rest.dto;

public record AuthToken(
    String jwt,
    String jwtRefresh,
    boolean available
) {}
