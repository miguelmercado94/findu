package com.findu.validacion.application.port.out;

import com.findu.validacion.domain.model.PhoneOtpRecord;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface PhoneOtpRepositoryPort {
    Mono<PhoneOtpRecord> saveOtp(PhoneOtpRecord record);
    Mono<PhoneOtpRecord> findLatestOtp(String phoneKey, String processCode);
    Mono<Void> markPhoneVerified(String phoneKey, String processCode, Duration ttl);
    Mono<Boolean> isPhoneVerified(String phoneKey, String processCode);
    Mono<Void> deleteOtp(String phoneKey, String processCode);
}
