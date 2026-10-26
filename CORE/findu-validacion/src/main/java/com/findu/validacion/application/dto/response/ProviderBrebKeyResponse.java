package com.findu.validacion.application.dto.response;

import java.time.Instant;

public record ProviderBrebKeyResponse(
        String id,
        Long providerId,
        String keyType,
        String keyValue,
        String maskedKey,
        String bank,
        String phone,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
