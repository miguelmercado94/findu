package com.findu.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OnboardingResponse(
        Long userId,
        Long perfilId,
        TokensData tokens,
        String estadoActivacion
) {
    public record TokensData(
            String accessToken,
            String refreshToken,
            String tokenType
    ) {}
}
