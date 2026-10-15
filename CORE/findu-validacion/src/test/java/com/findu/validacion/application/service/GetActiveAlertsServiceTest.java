package com.findu.validacion.application.service;

import com.findu.validacion.application.dto.response.ActiveAlertDto;
import com.findu.validacion.application.port.out.EndpointValidationConfigRepositoryPort;
import com.findu.validacion.application.port.out.ProcessValidationInstanceRepositoryPort;
import com.findu.validacion.application.port.out.ValidationDefinitionRepositoryPort;
import com.findu.validacion.domain.enums.ExecutionType;
import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.enums.ValidationStatus;
import com.findu.validacion.domain.model.EndpointValidationConfig;
import com.findu.validacion.domain.model.ProcessValidationInstance;
import com.findu.validacion.domain.model.ValidationDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActiveAlertsServiceTest {

    @Mock
    private ProcessValidationInstanceRepositoryPort instanceRepo;

    @Mock
    private EndpointValidationConfigRepositoryPort configRepo;

    @Mock
    private ValidationDefinitionRepositoryPort defRepo;

    @InjectMocks
    private GetActiveAlertsService service;

    private EndpointValidationConfig sampleConfig;
    private ProcessValidationInstance emailPendingInstance;
    private ProcessValidationInstance bioPendingInstance;
    private ValidationDefinition emailDef;
    private ValidationDefinition bioDef;

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

        emailPendingInstance = new ProcessValidationInstance(
                "inst-email",
                "USER",
                "10",
                "ONBOARDING_PROVEEDOR",
                "EMAIL_VERIFICATION",
                ValidationMode.PASSIVE,
                ValidationStatus.PENDING,
                Map.of(),
                null,
                "uuid-alert-email-1",
                Instant.now(),
                Instant.now(),
                "SYSTEM",
                "SYSTEM"
        );

        bioPendingInstance = new ProcessValidationInstance(
                "inst-bio",
                "USER",
                "10",
                "ONBOARDING_PROVEEDOR",
                "BIOMETRIC_FACIAL",
                ValidationMode.PASSIVE,
                ValidationStatus.PENDING,
                Map.of(),
                null,
                "uuid-alert-bio-2",
                Instant.now(),
                Instant.now(),
                "SYSTEM",
                "SYSTEM"
        );

        emailDef = new ValidationDefinition(
                "def-1",
                "EMAIL_VERIFICATION",
                "Verificación de correo",
                "Descripción email",
                ExecutionType.CLIENT_ACTION,
                "/api/v1/auth/email/verify",
                Map.of(),
                "PERFIL_CORREO_PENDIENTE"
        );

        bioDef = new ValidationDefinition(
                "def-2",
                "BIOMETRIC_FACIAL",
                "Validación biométrica",
                "Descripción biométrica",
                ExecutionType.CLIENT_ACTION,
                "/api/v1/auth/biometrics/verify",
                Map.of(),
                "PERFIL_BIOMETRIA_PENDIENTE"
        );
    }

    @Test
    @DisplayName("Usuario con 2 instances PENDING (EMAIL_VERIFICATION, BIOMETRIC_FACIAL) -> devuelve 2 alertas activas")
    void getActiveAlerts_shouldReturnTwoActiveAlerts_whenTwoInstancesPending() {
        when(instanceRepo.findByEntityAndStatus("USER", "10", ValidationStatus.PENDING))
                .thenReturn(Flux.just(emailPendingInstance, bioPendingInstance));
        when(configRepo.findByProcessCode("ONBOARDING_PROVEEDOR")).thenReturn(Mono.just(sampleConfig));
        when(defRepo.findByValidationCode("EMAIL_VERIFICATION")).thenReturn(Mono.just(emailDef));
        when(defRepo.findByValidationCode("BIOMETRIC_FACIAL")).thenReturn(Mono.just(bioDef));

        StepVerifier.create(service.getActiveAlerts("USER", "10"))
                .assertNext(alerts -> {
                    assertThat(alerts).hasSize(2);

                    ActiveAlertDto first = alerts.get(0);
                    assertThat(first.codigo()).isEqualTo("PERFIL_CORREO_PENDIENTE");
                    assertThat(first.validationCode()).isEqualTo("EMAIL_VERIFICATION");
                    assertThat(first.alertUuid()).isEqualTo("uuid-alert-email-1");
                    assertThat(first.active()).isTrue();
                    assertThat(first.titulo()).isEqualTo("Confirma tu correo");
                    assertThat(first.prioridad()).isEqualTo(1);

                    ActiveAlertDto second = alerts.get(1);
                    assertThat(second.codigo()).isEqualTo("PERFIL_BIOMETRIA_PENDIENTE");
                    assertThat(second.validationCode()).isEqualTo("BIOMETRIC_FACIAL");
                    assertThat(second.alertUuid()).isEqualTo("uuid-alert-bio-2");
                    assertThat(second.active()).isTrue();
                    assertThat(second.titulo()).isEqualTo("Completa tu validación biométrica");
                    assertThat(second.ctaLabel()).isEqualTo("Validar ahora");
                    assertThat(second.ctaAction()).isEqualTo("findu://perfil/verificacion-biometrica");
                    assertThat(second.prioridad()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Si una validación pasa a PASSED -> solo devuelve 1 alerta activa")
    void getActiveAlerts_shouldReturnOnlyOneActiveAlert_whenOnePassesToPassed() {
        // Al haber pasado EMAIL_VERIFICATION a PASSED, instanceRepo con status=PENDING solo devuelve BIOMETRIC_FACIAL
        when(instanceRepo.findByEntityAndStatus("USER", "10", ValidationStatus.PENDING))
                .thenReturn(Flux.just(bioPendingInstance));
        when(configRepo.findByProcessCode("ONBOARDING_PROVEEDOR")).thenReturn(Mono.just(sampleConfig));
        when(defRepo.findByValidationCode("BIOMETRIC_FACIAL")).thenReturn(Mono.just(bioDef));

        StepVerifier.create(service.getActiveAlerts("USER", "10"))
                .assertNext(alerts -> {
                    assertThat(alerts).hasSize(1);
                    ActiveAlertDto alert = alerts.getFirst();
                    assertThat(alert.codigo()).isEqualTo("PERFIL_BIOMETRIA_PENDIENTE");
                    assertThat(alert.validationCode()).isEqualTo("BIOMETRIC_FACIAL");
                    assertThat(alert.alertUuid()).isEqualTo("uuid-alert-bio-2");
                    assertThat(alert.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Instancia PASSIVE sin alertTemplateCode no se incluye en las alertas de UI")
    void getActiveAlerts_shouldExcludePassiveWithoutAlertTemplateCode() {
        ProcessValidationInstance judicialInstance = new ProcessValidationInstance(
                "inst-judicial",
                "USER",
                "10",
                "ONBOARDING_PROVEEDOR",
                "JUDICIAL_BACKGROUND",
                ValidationMode.PASSIVE,
                ValidationStatus.PENDING,
                Map.of(),
                null,
                "uuid-judicial",
                Instant.now(),
                Instant.now(),
                "SYSTEM",
                "SYSTEM"
        );
        ValidationDefinition judicialDef = new ValidationDefinition(
                "def-3",
                "JUDICIAL_BACKGROUND",
                "Antecedentes",
                "Desc",
                ExecutionType.BACKOFFICE,
                null,
                Map.of(),
                null // Sin alertTemplateCode
        );

        when(instanceRepo.findByEntityAndStatus("USER", "10", ValidationStatus.PENDING))
                .thenReturn(Flux.just(judicialInstance));
        when(configRepo.findByProcessCode("ONBOARDING_PROVEEDOR")).thenReturn(Mono.just(sampleConfig));
        when(defRepo.findByValidationCode("JUDICIAL_BACKGROUND")).thenReturn(Mono.just(judicialDef));

        StepVerifier.create(service.getActiveAlerts("USER", "10"))
                .assertNext(alerts -> assertThat(alerts).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Usuario sin instancias pendientes devuelve lista vacía 200")
    void getActiveAlerts_shouldReturnEmptyList_whenNoInstancesPending() {
        when(instanceRepo.findByEntityAndStatus("USER", "99", ValidationStatus.PENDING))
                .thenReturn(Flux.empty());

        StepVerifier.create(service.getActiveAlerts("USER", "99"))
                .assertNext(alerts -> assertThat(alerts).isEmpty())
                .verifyComplete();
    }
}
