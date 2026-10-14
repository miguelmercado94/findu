package com.findu.security.dto.internal;

public record InternalCreateUserRequest(
        String username,
        String email,
        String password,
        String phone,
        String codPhoneInternational,
        String roleName,
        Boolean isFederated,
        String provider,
        String providerUserId
) {
}
