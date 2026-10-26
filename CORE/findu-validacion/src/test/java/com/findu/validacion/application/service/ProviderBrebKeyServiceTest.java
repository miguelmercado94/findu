package com.findu.validacion.application.service;

import com.findu.validacion.application.dto.request.RegisterBrebKeyRequest;
import com.findu.validacion.application.dto.request.UpdateBrebKeyRequest;
import com.findu.validacion.application.port.out.ProcessValidationInstanceRepositoryPort;
import com.findu.validacion.application.port.out.ProviderBrebKeyRepositoryPort;
import com.findu.validacion.domain.exception.BrebKeyNotFoundException;
import com.findu.validacion.domain.exception.InvalidOtpException;
import com.findu.validacion.domain.model.ProcessValidationInstance;
import com.findu.validacion.domain.model.ProviderBrebKey;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderBrebKeyServiceTest {

    @Mock
    private ProviderBrebKeyRepositoryPort brebKeyRepo;

    @Mock
    private ProcessValidationInstanceRepositoryPort instanceRepo;

    @Mock
    private PhoneOtpService phoneOtpService;

    @InjectMocks
    private ProviderBrebKeyService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "devOtpCode", "123456");
    }

    @Test
    @DisplayName("registerKey vincula llave Bre-B y genera instancia de validación PASSED con OTP")
    void registerKey_shouldSucceed_whenOtpIsValid() {
        RegisterBrebKeyRequest request = new RegisterBrebKeyRequest(
                2L,
                "CELULAR",
                "+57 300 376 3300",
                "Bancolombia",
                "3003763300",
                "+57",
                "123456"
        );

        when(brebKeyRepo.findActiveByProviderId(2L)).thenReturn(Mono.empty());
        when(brebKeyRepo.save(any(ProviderBrebKey.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(instanceRepo.findByEntityAndProcessAndValidation(eq("USER"), eq("2"), eq("VINCULACION_LLAVE_BREB"), any()))
                .thenReturn(Mono.empty());
        when(instanceRepo.save(any(ProcessValidationInstance.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.registerKey(request))
                .assertNext(res -> {
                    assertThat(res.providerId()).isEqualTo(2L);
                    assertThat(res.keyType()).isEqualTo("CELULAR");
                    assertThat(res.bank()).isEqualTo("Bancolombia");
                    assertThat(res.status()).isEqualTo("ACTIVE");
                    assertThat(res.maskedKey()).contains("***");
                })
                .verifyComplete();

        verify(brebKeyRepo, times(1)).save(any(ProviderBrebKey.class));
        verify(instanceRepo, times(1)).save(any(ProcessValidationInstance.class));
    }

    @Test
    @DisplayName("registerKey rechaza con InvalidOtpException si el OTP es inválido")
    void registerKey_shouldFail_whenOtpIsInvalid() {
        RegisterBrebKeyRequest request = new RegisterBrebKeyRequest(
                2L,
                "CELULAR",
                "+57 300 376 3300",
                "Bancolombia",
                "3003763300",
                "+57",
                "999999"
        );

        when(phoneOtpService.verifyOtp(any()))
                .thenReturn(Mono.just(new com.findu.validacion.application.dto.response.VerifyPhoneOtpResponse(false, "OTP inválido")));

        StepVerifier.create(service.registerKey(request))
                .expectError(InvalidOtpException.class)
                .verify();

        verify(brebKeyRepo, never()).save(any());
    }

    @Test
    @DisplayName("getKey devuelve la llave activa del proveedor")
    void getKey_shouldReturnActiveKey() {
        ProviderBrebKey key = new ProviderBrebKey(
                "key-1",
                2L,
                "CELULAR",
                "+57 300 376 3300",
                "Bancolombia",
                "3003763300",
                "+57",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        when(brebKeyRepo.findActiveByProviderId(2L)).thenReturn(Mono.just(key));

        StepVerifier.create(service.getKey(2L))
                .assertNext(res -> {
                    assertThat(res.providerId()).isEqualTo(2L);
                    assertThat(res.bank()).isEqualTo("Bancolombia");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getKey arroja BrebKeyNotFoundException si no existe llave activa")
    void getKey_shouldThrow_whenNotFound() {
        when(brebKeyRepo.findActiveByProviderId(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.getKey(99L))
                .expectError(BrebKeyNotFoundException.class)
                .verify();
    }
}
