package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.externalapi.CorePerfilPort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.service.AuthenticatedUserResolver;
import com.findu.security.application.usecase.ActivationProgressService;
import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.ProviderActivationState;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.request.BiometricSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitBiometricUseCaseImplTest {

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;
    @Mock
    private CorePerfilPort corePerfilPort;
    @Mock
    private ProviderActivationRepositoryPort activationRepository;
    @Mock
    private ActivationProgressService activationProgressService;

    private SubmitBiometricUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SubmitBiometricUseCaseImpl(authenticatedUserResolver, corePerfilPort,
                activationRepository, activationProgressService);
    }

    private Usuario user() {
        Usuario u = new Usuario();
        u.setId(42L);
        u.setUsername("prov");
        return u;
    }

    private BiometricSubmitRequest request(String nombre, String fecha, boolean matched) {
        return new BiometricSubmitRequest(nombre, fecha, new BigDecimal("0.97"), matched,
                "https://cdn.findu.example/frontal.jpg");
    }

    @Test
    void submit_malformedDate_400() {
        StepVerifier.create(useCase.submit(request("Carlos Mendoza", "not-a-date", true)))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void submit_futureDate_400() {
        String future = LocalDate.now().plusDays(1).toString();
        StepVerifier.create(useCase.submit(request("Carlos Mendoza", future, true)))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void submit_perfilMissing_409() {
        when(authenticatedUserResolver.resolve()).thenReturn(Mono.just(user()));
        when(corePerfilPort.fetchByAuthUserId(42L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.submit(request("Carlos Mendoza", "1994-11-01", true)))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.CONFLICT
                        && "perfil_core_requerido".equals(rse.getReason()))
                .verify();
    }

    @Test
    void submit_nameMismatch_businessRejection200() {
        when(authenticatedUserResolver.resolve()).thenReturn(Mono.just(user()));
        when(corePerfilPort.fetchByAuthUserId(42L)).thenReturn(Mono.just(
                new CorePerfilPort.PerfilProveedor(7L, "Carlos Perez", LocalDate.of(1994, 11, 1), null)));
        ProviderActivation a = new ProviderActivation();
        a.setEstado(ProviderActivationState.PENDIENTE_VALIDACIONES);
        when(activationRepository.upsertValidation(eq(42L), any(), any(), eq("nombre_no_coincide")))
                .thenReturn(Mono.just(a));

        StepVerifier.create(useCase.submit(request("Carlos Mendoza", "1994-11-01", true)))
                .expectNextMatches(r -> !r.biometriaOk() && "nombre_no_coincide".equals(r.motivo()))
                .verifyComplete();

        verify(activationProgressService, never()).recompute(anyLong());
    }

    @Test
    void submit_dateMismatch_businessRejection200() {
        when(authenticatedUserResolver.resolve()).thenReturn(Mono.just(user()));
        when(corePerfilPort.fetchByAuthUserId(42L)).thenReturn(Mono.just(
                new CorePerfilPort.PerfilProveedor(7L, "Carlos Mendoza", LocalDate.of(1990, 1, 1), null)));
        when(activationRepository.upsertValidation(eq(42L), any(), any(), eq("fecha_no_coincide")))
                .thenReturn(Mono.just(new ProviderActivation()));

        StepVerifier.create(useCase.submit(request("Carlos Mendoza", "1994-11-01", true)))
                .expectNextMatches(r -> !r.biometriaOk() && "fecha_no_coincide".equals(r.motivo()))
                .verifyComplete();
    }

    @Test
    void submit_faceNotMatched_businessRejection200() {
        when(authenticatedUserResolver.resolve()).thenReturn(Mono.just(user()));
        when(corePerfilPort.fetchByAuthUserId(42L)).thenReturn(Mono.just(
                new CorePerfilPort.PerfilProveedor(7L, "Carlos Mendoza", LocalDate.of(1994, 11, 1), null)));
        when(activationRepository.upsertValidation(eq(42L), any(), any(), eq("rostro_no_coincide")))
                .thenReturn(Mono.just(new ProviderActivation()));

        StepVerifier.create(useCase.submit(request("Carlos Mendoza", "1994-11-01", false)))
                .expectNextMatches(r -> !r.biometriaOk() && "rostro_no_coincide".equals(r.motivo()))
                .verifyComplete();
    }

    @Test
    void submit_allPass_insertsCapture_marksBiometria_pushesPhoto_recomputes() {
        when(authenticatedUserResolver.resolve()).thenReturn(Mono.just(user()));
        when(corePerfilPort.fetchByAuthUserId(42L)).thenReturn(Mono.just(
                new CorePerfilPort.PerfilProveedor(7L, "José Peña", LocalDate.of(1994, 11, 1), null)));
        when(activationRepository.insertBiometricCapture(eq(42L), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just("https://cdn.findu.example/frontal.jpg"));
        when(activationRepository.upsertValidation(eq(42L), any(), any(), eq(null)))
                .thenReturn(Mono.just(new ProviderActivation()));
        when(corePerfilPort.pushProfilePhoto(eq(7L), any(), any())).thenReturn(Mono.empty());
        ProviderActivation result = new ProviderActivation();
        result.setEstado(ProviderActivationState.EN_REVISION_SOPORTE);
        result.setBiometriaOk(true);
        when(activationProgressService.recompute(42L)).thenReturn(Mono.just(result));

        StepVerifier.create(useCase.submit(request("jose pena", "1994-11-01", true)))
                .expectNextMatches(r -> r.biometriaOk() && r.motivo() == null)
                .verifyComplete();

        verify(corePerfilPort).pushProfilePhoto(eq(7L), any(), eq("https://cdn.findu.example/frontal.jpg"));
        verify(activationProgressService).recompute(42L);
    }
}
