package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.request.SendPhoneOtpRequest;
import com.findu.validacion.application.dto.request.VerifyPhoneOtpRequest;
import com.findu.validacion.application.dto.response.SendPhoneOtpResponse;
import com.findu.validacion.application.dto.response.VerifyPhoneOtpResponse;
import com.findu.validacion.application.port.in.ValidatePhoneOtpUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneOtpValidationControllerTest {

    @Mock
    private ValidatePhoneOtpUseCase otpUseCase;

    @InjectMocks
    private PhoneOtpValidationController controller;

    @Test
    @DisplayName("POST /api/v1/validations/otp/send-phone-otp retorna 200 OK con SendPhoneOtpResponse")
    void sendPhoneOtp_shouldReturnOk() {
        SendPhoneOtpRequest request = new SendPhoneOtpRequest("3001234567", "+57", "VINCULACION_LLAVE_BREB");
        SendPhoneOtpResponse response = new SendPhoneOtpResponse("SENT", 300, "Código OTP enviado exitosamente");

        when(otpUseCase.sendOtp(any(SendPhoneOtpRequest.class))).thenReturn(Mono.just(response));

        Mono<ResponseEntity<SendPhoneOtpResponse>> result = controller.sendPhoneOtp(request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody().status()).isEqualTo("SENT");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("POST /api/v1/validations/otp/verify-phone-otp retorna 200 OK con VerifyPhoneOtpResponse")
    void verifyPhoneOtp_shouldReturnOk() {
        VerifyPhoneOtpRequest request = new VerifyPhoneOtpRequest("3001234567", "+57", "123456", "VINCULACION_LLAVE_BREB");
        VerifyPhoneOtpResponse response = new VerifyPhoneOtpResponse(true, "Código OTP verificado exitosamente");

        when(otpUseCase.verifyOtp(any(VerifyPhoneOtpRequest.class))).thenReturn(Mono.just(response));

        Mono<ResponseEntity<VerifyPhoneOtpResponse>> result = controller.verifyPhoneOtp(request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody().verified()).isTrue();
                })
                .verifyComplete();
    }
}
