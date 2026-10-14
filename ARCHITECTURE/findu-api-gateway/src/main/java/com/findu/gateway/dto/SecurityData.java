package com.findu.gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record SecurityData(
        @NotBlank String username,
        @NotBlank String email,
        String password,
        @NotBlank String phone,
        String codPhoneInternational,
        Boolean phoneOtpVerified
) {
}
