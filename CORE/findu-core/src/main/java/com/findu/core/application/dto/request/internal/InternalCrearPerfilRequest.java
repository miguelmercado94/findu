package com.findu.core.application.dto.request.internal;

public record InternalCrearPerfilRequest(
        Long authUserId,
        String flowType,
        String username,
        String email,
        String phone,
        String codPhoneInternational,
        InternalPersonaRequest persona,
        InternalDireccionRequest direccion
) {
}
