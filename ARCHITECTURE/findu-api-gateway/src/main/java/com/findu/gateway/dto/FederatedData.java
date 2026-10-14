package com.findu.gateway.dto;

public record FederatedData(
        String provider,
        String providerUserId,
        String idToken
) {
}
