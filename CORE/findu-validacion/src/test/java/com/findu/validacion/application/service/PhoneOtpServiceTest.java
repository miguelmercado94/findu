package com.findu.validacion.application.service;

import com.findu.validacion.application.dto.request.SendPhoneOtpRequest;
import com.findu.validacion.application.dto.request.VerifyPhoneOtpRequest;
import com.findu.validacion.application.port.out.PhoneOtpRepositoryPort;
import com.findu.validacion.domain.model.PhoneOtpRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneOtpServiceTest {

    @Mock
    private PhoneOtpRepositoryPort otpRepository;

    @InjectMocks
    private PhoneOtpService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "devOtpCode", "123456");
        ReflectionTestUtils.setField(service, "otpExpirySeconds", 300);
    }

    @Test
    @DisplayName("sendOtp genera y almacena OTP en MongoDB con estado SENT")
    void sendOtp_shouldGenerateAndSaveOtp() {
        when(otpRepository.saveOtp(any(PhoneOtpRecord.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        SendPhoneOtpRequest request = new SendPhoneOtpRequest("3001234567", "+57", "VINCULACION_LLAVE_BREB");

        StepVerifier.create(service.sendOtp(request))
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo("SENT");
                    assertThat(response.expiresInSeconds()).isEqualTo(300);
                    assertThat(response.message()).contains("+573001234567");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("verifyOtp aprueba exitosamente si se ingresa el código estático de desarrollo")
    void verifyOtp_shouldPass_withDevStaticCode() {
        when(otpRepository.markPhoneVerified(eq("+573001234567"), eq("VINCULACION_LLAVE_BREB"), any()))
                .thenReturn(Mono.empty());

        VerifyPhoneOtpRequest request = new VerifyPhoneOtpRequest("3001234567", "+57", "123456", "VINCULACION_LLAVE_BREB");

        StepVerifier.create(service.verifyOtp(request))
                .assertNext(response -> {
                    assertThat(response.verified()).isTrue();
                    assertThat(response.message()).contains("exitosamente");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("verifyOtp rechaza si el código es incorrecto")
    void verifyOtp_shouldFail_whenCodeIsWrong() {
        PhoneOtpRecord record = new PhoneOtpRecord(
                "otp-1",
                "+573001234567",
                "654321",
                "VINCULACION_LLAVE_BREB",
                Instant.now().plusSeconds(300),
                false,
                null,
                Instant.now()
        );

        when(otpRepository.findLatestOtp("+573001234567", "VINCULACION_LLAVE_BREB"))
                .thenReturn(Mono.just(record));

        VerifyPhoneOtpRequest request = new VerifyPhoneOtpRequest("3001234567", "+57", "999999", "VINCULACION_LLAVE_BREB");

        StepVerifier.create(service.verifyOtp(request))
                .assertNext(response -> {
                    assertThat(response.verified()).isFalse();
                    assertThat(response.message()).isEqualTo("Código OTP inválido");
                })
                .verifyComplete();
    }
}
