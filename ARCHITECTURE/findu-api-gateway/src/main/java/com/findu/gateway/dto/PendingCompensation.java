package com.findu.gateway.dto;

import java.time.Instant;

public record PendingCompensation(
        String compensationId,
        String targetService,
        String action,
        Long targetId,
        int retryCount,
        long createdAtEpochSeconds
) {
    public static PendingCompensation of(String targetService, String action, Long targetId) {
        return new PendingCompensation(
                java.util.UUID.randomUUID().toString(),
                targetService,
                action,
                targetId,
                0,
                Instant.now().getEpochSecond()
        );
    }
}
