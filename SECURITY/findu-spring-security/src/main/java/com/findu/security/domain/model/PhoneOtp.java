package com.findu.security.domain.model;

/**
 * OTP de verificación de teléfono almacenado en el store (Redis o in-memory) serializado a JSON.
 *
 * @param codeHash             hash del código OTP (nunca se guarda en claro)
 * @param expiresAtEpochSeconds instante de expiración en segundos epoch
 * @param attempts             número de intentos de verificación realizados
 */
public record PhoneOtp(String codeHash, long expiresAtEpochSeconds, int attempts) {
}
