package com.findu.security.dto.internal;

public record InternalTokensResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static InternalTokensResponse of(String accessToken, String refreshToken) {
        return new InternalTokensResponse(accessToken, refreshToken, "Bearer");
    }
}
