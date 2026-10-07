package com.findu.security.application.usecase;

import reactor.core.publisher.Mono;

/**
 * Caso de uso: generar y enviar el OTP de verificación de teléfono del proveedor.
 */
public interface SendPhoneOtpUseCase {

    /**
     * Genera un OTP de 6 dígitos, lo persiste con TTL en el store y lo envía por el
     * microservicio de notificaciones. Fire-and-forget: no revela si el teléfono existe.
     *
     * @param phone                 número de teléfono
     * @param codPhoneInternational código de país (por defecto +57)
     * @return Mono vacío
     */
    Mono<Void> sendOtp(String phone, String codPhoneInternational);
}
