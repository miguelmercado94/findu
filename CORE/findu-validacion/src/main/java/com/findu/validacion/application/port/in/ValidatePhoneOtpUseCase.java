package com.findu.validacion.application.port.in;

import com.findu.validacion.application.dto.request.SendPhoneOtpRequest;
import com.findu.validacion.application.dto.request.VerifyPhoneOtpRequest;
import com.findu.validacion.application.dto.response.SendPhoneOtpResponse;
import com.findu.validacion.application.dto.response.VerifyPhoneOtpResponse;
import reactor.core.publisher.Mono;

public interface ValidatePhoneOtpUseCase {
    Mono<SendPhoneOtpResponse> sendOtp(SendPhoneOtpRequest request);
    Mono<VerifyPhoneOtpResponse> verifyOtp(VerifyPhoneOtpRequest request);
}
