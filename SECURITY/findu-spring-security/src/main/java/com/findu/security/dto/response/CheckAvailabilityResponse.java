package com.findu.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckAvailabilityResponse(
        boolean available,
        String reason
) {
    public static CheckAvailabilityResponse ofAvailable() {
        return new CheckAvailabilityResponse(true, null);
    }

    public static CheckAvailabilityResponse ofUnavailable(String reason) {
        return new CheckAvailabilityResponse(false, reason);
    }
}
