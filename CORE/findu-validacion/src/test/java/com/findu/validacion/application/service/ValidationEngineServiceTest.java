package com.findu.validacion.application.service;

import com.findu.validacion.application.dto.event.PendingValidationAlertEvent;
import com.findu.validacion.application.dto.event.ValidationResolvedEvent;
import com.findu.validacion.application.dto.request.CheckActiveRequest;
import com.findu.validacion.application.dto.request.CompleteProcessRequest;
import com.findu.validacion.application.dto.request.InspectRequest;
import com.findu.validacion.application.dto.request.SupportResolveRequest;
import com.findu.validacion.application.dto.request.ValidationEvidenceRequest;
import com.findu.validacion.application.port.out.AlertEventPublisherPort;
import com.findu.validacion.application.port.out.EndpointValidationConfigRepositoryPort;
import com.findu.validacion.application.port.out.MonitoredEndpointRepositoryPort;
import com.findu.validacion.application.port.out.ProcessValidationInstanceRepositoryPort;
import com.findu.validacion.application.port.out.ValidationDefinitionRepositoryPort;
import com.findu.validacion.domain.enums.ExecutionType;
import com.findu.validacion.domain.enums.HttpMethod;
import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.enums.ValidationStatus;
import com.findu.validacion.domain.exception.EndpointNotMonitoredException;
import com.findu.validacion.domain.exception.ValidationNotFoundException;
import com.findu.validacion.domain.model.EndpointValidationConfig;
import com.findu.validacion.domain.model.MonitoredEndpoint;
import com.findu.validacion.domain.model.ProcessValidationInstance;
import com.findu.validacion.domain.model.ValidationDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationEngineServiceTest {

    @Mock
    private MonitoredEndpointRepositoryPort endpointRepo;

    @Mock
    private ValidationDefinitionRepositoryPort defRepo;

    @Mock
    private EndpointValidationConfigRepositoryPort configRepo;

    @Mock
    private ProcessValidationInstanceRepositoryPort instanceRepo;

    @Mock
    private AlertEventPublisherPort alertPublisher;

    @InjectMocks
    private ValidationEngineService service;

    private EndpointValidationConfig sampleConfig;

    @BeforeEach
    void setUp() {
        sampleConfig = new EndpointValidationConfig(
                "cfg-1",
                "ONBOARDING_PROVEEDOR",
                List.of(
                        new EndpointValidationConfig.ConfigItem("OTP_PHONE_SMS", ValidationMode.ACTIVE, 1, true, "otp.required"),
                        new EndpointValidationConfig.ConfigItem("EMAIL_VERIFICATION", ValidationMode.PASSIVE, 2, false, "email.pending"),
                        new EndpointValidationConfig.ConfigItem("BIOMETRIC_FACIAL", ValidationMode.PASSIVE, 3, false, "bio.pending"),
                        new EndpointValidationConfig.ConfigItem("JUDICIAL_BACKGROUND", ValidationMode.PASSIVE, 4, false, "legal.pending")
                )
        );
    }

    @Test
    @DisplayName("complete-process genera instancias PENDING y publica PendingValidationAlertEvent por cada PASSIVE no satisfecha")
    void complete_shouldCreatePendingInstancesAndPublishAlerts_forUnsatisfiedPassiveValidations() {
        when(configRepo.findByProcessCode("ONBOARDING_PROVEEDOR")).thenReturn(Mono.just(sampleConfig));
        when(instanceRepo.findByEntityAndProcessAndValidation(eq("USER"), eq("2"), eq("ONBOARDING_PROVEEDOR"), any()))
                .thenReturn(Mono.empty());
        when(instanceRepo.save(any(ProcessValidationInstance.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(defRepo.findByValidationCode("EMAIL_VERIFICATION")).thenReturn(Mono.just(new ValidationDefinition(
                "def-1", "EMAIL_VERIFICATION", "Email", "Desc", ExecutionType.CLIENT_ACTION,
                "/api/v1/auth/email/verify", Map.of(), "PERFIL_CORREO_PENDIENTE")));
        when(defRepo.findByValidationCode("BIOMETRIC_FACIAL")).thenReturn(Mono.just(new ValidationDefinition(
                "def-2", "BIOMETRIC_FACIAL", "Bio", "Desc", ExecutionType.CLIENT_ACTION,
                "/api/v1/auth/biometrics/verify", Map.of(), "PERFIL_BIOMETRIA_PENDIENTE")));
        when(defRepo.findByValidationCode("JUDICIAL_BACKGROUND")).thenReturn(Mono.just(new ValidationDefinition(
                "def-3", "JUDICIAL_BACKGROUND", "Judicial", "Desc", ExecutionType.BACKOFFICE,
                null, Map.of(), null)));
        when(alertPublisher.publishPending(any(PendingValidationAlertEvent.class))).thenReturn(Mono.empty());

        CompleteProcessRequest request = new CompleteProcessRequest(
                "ONBOARDING_PROVEEDOR",
                "2",
                2L,
                "USER",
                List.of("OTP_PHONE_SMS"),
                List.of(new ValidationEvidenceRequest("BIOMETRIC_FACIAL", Map.of("faceMatchScore", 0.0)))
        );

        StepVerifier.create(service.complete(request))
                .assertNext(response -> {
                    assertThat(response.processCode()).isEqualTo("ONBOARDING_PROVEEDOR");
                    assertThat(response.pending()).containsExactlyInAnyOrder(
                            "EMAIL_VERIFICATION", "BIOMETRIC_FACIAL", "JUDICIAL_BACKGROUND");
                    assertThat(response.alertsEmitted()).isEqualTo(3);
                })
                .verifyComplete();

        verify(instanceRepo, times(3)).save(any(ProcessValidationInstance.class));
        verify(alertPublisher, times(3)).publishPending(any(PendingValidationAlertEvent.class));
    }

    @Test
    @DisplayName("support/resolve marca PASSED y publica ValidationResolvedEvent")
    void supportResolve_shouldMarkInstancePassedAndPublishResolvedEvent() {
        ProcessValidationInstance existing = new ProcessValidationInstance(
                "inst-1",
                "USER",
                "2",
                "ONBOARDING_PROVEEDOR",
                "JUDICIAL_BACKGROUND",
                ValidationMode.PASSIVE,
                ValidationStatus.PENDING,
                new HashMap<>(Map.of("initial", "data")),
                null,
                "alert-uuid-1234",
                Instant.now(),
                Instant.now(),
                "SYSTEM",
                "SYSTEM"
        );

        when(instanceRepo.findByEntityAndProcessAndValidation("USER", "2", "ONBOARDING_PROVEEDOR", "JUDICIAL_BACKGROUND"))
                .thenReturn(Mono.just(existing));
        when(instanceRepo.save(any(ProcessValidationInstance.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(alertPublisher.publishResolved(any(ValidationResolvedEvent.class))).thenReturn(Mono.empty());

        SupportResolveRequest request = new SupportResolveRequest(
                "ONBOARDING_PROVEEDOR",
                "2",
                "USER",
                "JUDICIAL_BACKGROUND",
                "PASSED",
                1L,
                "antecedentes OK"
        );

        StepVerifier.create(service.resolve(request))
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo("PASSED");
                    assertThat(response.alertUuid()).isEqualTo("alert-uuid-1234");
                })
                .verifyComplete();

        ArgumentCaptor<ProcessValidationInstance> instanceCaptor = ArgumentCaptor.forClass(ProcessValidationInstance.class);
        verify(instanceRepo).save(instanceCaptor.capture());
        ProcessValidationInstance saved = instanceCaptor.getValue();
        assertThat(saved.status()).isEqualTo(ValidationStatus.PASSED);
        assertThat(saved.reviewerId()).isEqualTo(1L);
        assertThat(saved.evidence()).containsEntry("resolutionNote", "antecedentes OK");

        ArgumentCaptor<ValidationResolvedEvent> eventCaptor = ArgumentCaptor.forClass(ValidationResolvedEvent.class);
        verify(alertPublisher).publishResolved(eventCaptor.capture());
        ValidationResolvedEvent event = eventCaptor.getValue();
        assertThat(event.validationCode()).isEqualTo("JUDICIAL_BACKGROUND");
        assertThat(event.userId()).isEqualTo(2L);
        assertThat(event.alertUuid()).isEqualTo("alert-uuid-1234");
        assertThat(event.resolution()).isEqualTo("PASSED");
    }

    @Test
    @DisplayName("support/resolve lanza ValidationNotFoundException si la instancia no existe")
    void supportResolve_shouldThrowValidationNotFoundException_whenInstanceNotFound() {
        when(instanceRepo.findByEntityAndProcessAndValidation("USER", "99", "ONBOARDING_PROVEEDOR", "JUDICIAL_BACKGROUND"))
                .thenReturn(Mono.empty());

        SupportResolveRequest request = new SupportResolveRequest(
                "ONBOARDING_PROVEEDOR",
                "99",
                "USER",
                "JUDICIAL_BACKGROUND",
                "PASSED",
                1L,
                "ok"
        );

        StepVerifier.create(service.resolve(request))
                .expectError(ValidationNotFoundException.class)
                .verify();

        verify(alertPublisher, never()).publishResolved(any());
    }

    @Test
    @DisplayName("check-active bloquea si falta una validación ACTIVE obligatoria")
    void checkActive_shouldBlock_whenMandatoryActiveMissing() {
        when(configRepo.findByProcessCode("ONBOARDING_PROVEEDOR")).thenReturn(Mono.just(sampleConfig));

        CheckActiveRequest request = new CheckActiveRequest(
                "ONBOARDING_PROVEEDOR",
                "2",
                List.of() // Sin evidencias de OTP
        );

        StepVerifier.create(service.check(request))
                .assertNext(response -> {
                    assertThat(response.allowed()).isFalse();
                    assertThat(response.failed()).hasSize(1);
                    assertThat(response.failed().getFirst().validationCode()).isEqualTo("OTP_PHONE_SMS");
                    assertThat(response.failed().getFirst().errorMessageKey()).isEqualTo("otp.required");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("check-active aprueba si las validaciones ACTIVE obligatorias están satisfechas")
    void checkActive_shouldAllow_whenMandatoryActiveProvided() {
        when(configRepo.findByProcessCode("ONBOARDING_PROVEEDOR")).thenReturn(Mono.just(sampleConfig));

        CheckActiveRequest request = new CheckActiveRequest(
                "ONBOARDING_PROVEEDOR",
                "2",
                List.of(new ValidationEvidenceRequest("OTP_PHONE_SMS", Map.of("code", "123456")))
        );

        StepVerifier.create(service.check(request))
                .assertNext(response -> {
                    assertThat(response.allowed()).isTrue();
                    assertThat(response.failed()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("check-active es fail-open: si el proceso no está configurado, permite la operación")
    void checkActive_shouldAllowFailOpen_whenConfigNotFound() {
        when(configRepo.findByProcessCode("PROCESO_NO_MONITOREADO")).thenReturn(Mono.empty());

        CheckActiveRequest request = new CheckActiveRequest(
                "PROCESO_NO_MONITOREADO",
                "2",
                List.of()
        );

        StepVerifier.create(service.check(request))
                .assertNext(response -> {
                    assertThat(response.allowed()).isTrue();
                    assertThat(response.failed()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("inspect devuelve la configuración activa y pasiva del endpoint monitoreado")
    void inspect_shouldReturnConfig_whenEndpointIsMonitored() {
        MonitoredEndpoint endpoint = new MonitoredEndpoint(
                "ep-1",
                "findu-spring-security",
                HttpMethod.POST,
                "/api/v1/registration/onboarding",
                "ONBOARDING_PROVEEDOR",
                "Onboarding",
                true,
                Instant.now()
        );

        when(endpointRepo.findByServiceAndMethodAndPath("findu-spring-security", HttpMethod.POST, "/api/v1/registration/onboarding"))
                .thenReturn(Mono.just(endpoint));
        when(configRepo.findByProcessCode("ONBOARDING_PROVEEDOR")).thenReturn(Mono.just(sampleConfig));
        when(defRepo.findByValidationCodeIn(any())).thenReturn(Flux.just(
                new ValidationDefinition("d-1", "OTP_PHONE_SMS", "OTP", "Desc", ExecutionType.CLIENT_ACTION, "/api/v1/auth/phone/verify-otp", Map.of(), null),
                new ValidationDefinition("d-2", "EMAIL_VERIFICATION", "Email", "Desc", ExecutionType.CLIENT_ACTION, "/api/v1/auth/email/verify", Map.of(), "PERFIL_CORREO_PENDIENTE"),
                new ValidationDefinition("d-3", "BIOMETRIC_FACIAL", "Bio", "Desc", ExecutionType.CLIENT_ACTION, "/api/v1/auth/biometrics/verify", Map.of(), "PERFIL_BIOMETRIA_PENDIENTE"),
                new ValidationDefinition("d-4", "JUDICIAL_BACKGROUND", "Judicial", "Desc", ExecutionType.BACKOFFICE, null, Map.of(), null)
        ));

        InspectRequest request = new InspectRequest(
                "findu-spring-security",
                "POST",
                "/api/v1/registration/onboarding"
        );

        StepVerifier.create(service.inspect(request))
                .assertNext(response -> {
                    assertThat(response.processCode()).isEqualTo("ONBOARDING_PROVEEDOR");
                    assertThat(response.active()).hasSize(1);
                    assertThat(response.active().getFirst().validationCode()).isEqualTo("OTP_PHONE_SMS");
                    assertThat(response.active().getFirst().isMandatory()).isTrue();
                    assertThat(response.active().getFirst().resolutionEndpoint()).isEqualTo("/api/v1/auth/phone/verify-otp");

                    assertThat(response.passive()).hasSize(3);
                    assertThat(response.passive().stream().map(p -> p.validationCode()).toList())
                            .containsExactlyInAnyOrder("EMAIL_VERIFICATION", "BIOMETRIC_FACIAL", "JUDICIAL_BACKGROUND");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("inspect lanza EndpointNotMonitoredException si el endpoint no está registrado")
    void inspect_shouldThrowEndpointNotMonitoredException_whenEndpointNotFound() {
        when(endpointRepo.findByServiceAndMethodAndPath("otro-servicio", HttpMethod.POST, "/api/v1/test"))
                .thenReturn(Mono.empty());

        InspectRequest request = new InspectRequest("otro-servicio", "POST", "/api/v1/test");

        StepVerifier.create(service.inspect(request))
                .expectError(EndpointNotMonitoredException.class)
                .verify();
    }
}
