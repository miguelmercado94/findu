package com.findu.security.application.usecase;

import com.findu.security.dto.response.PhoneVerificationResponse;
import reactor.core.publisher.Mono;

/**
 * Caso de uso: validar el OTP de teléfono del proveedor y marcar el teléfono como verificado.
 */
public interface VerifyPhoneOtpUseCase {

    /**
     * Valida el código contra el store. Si es correcto y no ha expirado, marca
     * phoneVerified=true y avanza account_status a PENDING_EMAIL_VERIFICATION.
     *
     * @param phone                 número de teléfono
     * @param code                  código OTP ingresado
     * @param codPhoneInternational código de país (por defecto +57)
     * @return estado de verificación del teléfono
     */
    Mono<PhoneVerificationResponse> verifyOtp(String phone, String code, String codPhoneInternational);
}
