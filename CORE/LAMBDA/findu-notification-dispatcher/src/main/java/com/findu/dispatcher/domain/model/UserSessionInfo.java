package com.findu.dispatcher.domain.model;

import java.time.Instant;

public record UserSessionInfo(
        String userId,
        String sessionId,
        String nodeId,
        Instant connectedAt
) {}
