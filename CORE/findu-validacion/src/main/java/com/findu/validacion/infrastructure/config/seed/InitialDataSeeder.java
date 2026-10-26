package com.findu.validacion.infrastructure.config.seed;

import com.findu.validacion.application.port.out.EndpointValidationConfigRepositoryPort;
import com.findu.validacion.application.port.out.MonitoredEndpointRepositoryPort;
import com.findu.validacion.application.port.out.ValidationDefinitionRepositoryPort;
import com.findu.validacion.domain.enums.ExecutionType;
import com.findu.validacion.domain.enums.HttpMethod;
import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.model.EndpointValidationConfig;
import com.findu.validacion.domain.model.MonitoredEndpoint;
import com.findu.validacion.domain.model.ValidationDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialDataSeeder implements ApplicationRunner {

    private final MonitoredEndpointRepositoryPort endpointRepo;
    private final ValidationDefinitionRepositoryPort defRepo;
    private final EndpointValidationConfigRepositoryPort configRepo;

    public static final String ONBOARDING_PROCESS_CODE = "ONBOARDING_PROVEEDOR";
    public static final String BREB_VINCULACION_PROCESS_CODE = "VINCULACION_LLAVE_BREB";
    public static final String BREB_CAMBIO_PROCESS_CODE = "CAMBIO_LLAVE_BREB";

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking initial data seed for findu-validacion...");

        seedMonitoredEndpoints()
                .then(seedValidationDefinitions())
                .then(seedEndpointValidationConfigs())
                .doOnSuccess(v -> log.info("Initial data seed completed successfully"))
                .doOnError(e -> log.error("Error during initial data seed: {}", e.getMessage(), e))
                .subscribe();
    }

    public Mono<Void> seedMonitoredEndpoints() {
        return seedEndpointIfNotExists(new MonitoredEndpoint(
                null,
                "findu-spring-security",
                HttpMethod.POST,
                "/api/v1/registration/onboarding",
                ONBOARDING_PROCESS_CODE,
                "Registro de proveedor (saga onboarding)",
                true,
                Instant.now()
        ))
        .then(seedEndpointIfNotExists(new MonitoredEndpoint(
                null,
                "findu-validacion",
                HttpMethod.POST,
                "/api/v1/validations/bre-b-keys",
                BREB_VINCULACION_PROCESS_CODE,
                "Vinculación y registro de llave Bre-B para proveedor",
                true,
                Instant.now()
        )))
        .then(seedEndpointIfNotExists(new MonitoredEndpoint(
                null,
                "findu-validacion",
                HttpMethod.PUT,
                "/api/v1/validations/bre-b-keys",
                BREB_CAMBIO_PROCESS_CODE,
                "Actualización de llave Bre-B para proveedor",
                true,
                Instant.now()
        )));
    }

    private Mono<Void> seedEndpointIfNotExists(MonitoredEndpoint endpoint) {
        return endpointRepo.existsByProcessCode(endpoint.processCode())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        log.debug("Monitored endpoint for '{}' already exists, skipping", endpoint.processCode());
                        return Mono.empty();
                    }
                    return endpointRepo.save(endpoint)
                            .doOnSuccess(saved -> log.info("Seeded monitored endpoint for processCode='{}'", saved.processCode()))
                            .then();
                });
    }

    public Mono<Void> seedValidationDefinitions() {
        List<ValidationDefinition> definitions = List.of(
                new ValidationDefinition(
                        null,
                        "OTP_PHONE_SMS",
                        "Verificación de teléfono por OTP",
                        "Validación de número telefónico mediante código SMS",
                        ExecutionType.CLIENT_ACTION,
                        "/api/v1/auth/phone/verify-otp",
                        Map.of("type", "object", "properties", Map.of("code", Map.of("type", "string"))),
                        null
                ),
                new ValidationDefinition(
                        null,
                        "EMAIL_VERIFICATION",
                        "Verificación de correo electrónico",
                        "Validación de dirección de correo electrónico del usuario",
                        ExecutionType.CLIENT_ACTION,
                        "/api/v1/auth/email/verify",
                        Map.of(),
                        "PERFIL_CORREO_PENDIENTE"
                ),
                new ValidationDefinition(
                        null,
                        "BIOMETRIC_FACIAL",
                        "Validación biométrica facial",
                        "Validación de identidad mediante prueba de vida y reconocimiento facial",
                        ExecutionType.CLIENT_ACTION,
                        "/api/v1/auth/biometrics/verify",
                        Map.of(),
                        "PERFIL_BIOMETRIA_PENDIENTE"
                ),
                new ValidationDefinition(
                        null,
                        "JUDICIAL_BACKGROUND",
                        "Verificación de antecedentes judiciales",
                        "Revisión de antecedentes disciplinarios y legales por mesa de soporte",
                        ExecutionType.BACKOFFICE,
                        null,
                        Map.of(),
                        null
                )
        );

        return Flux.fromIterable(definitions)
                .flatMap(def -> defRepo.existsByValidationCode(def.validationCode())
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                log.debug("Validation definition '{}' already exists, skipping", def.validationCode());
                                return Mono.empty();
                            }
                            return defRepo.save(def)
                                    .doOnSuccess(saved -> log.info("Seeded validation definition '{}'", saved.validationCode()))
                                    .then();
                        })
                )
                .then();
    }

    public Mono<Void> seedEndpointValidationConfigs() {
        return seedConfigIfNotExists(new EndpointValidationConfig(
                null,
                ONBOARDING_PROCESS_CODE,
                List.of(
                        new EndpointValidationConfig.ConfigItem("OTP_PHONE_SMS", ValidationMode.ACTIVE, 1, true, "otp.required"),
                        new EndpointValidationConfig.ConfigItem("EMAIL_VERIFICATION", ValidationMode.PASSIVE, 2, false, "email.pending"),
                        new EndpointValidationConfig.ConfigItem("BIOMETRIC_FACIAL", ValidationMode.PASSIVE, 3, false, "bio.pending"),
                        new EndpointValidationConfig.ConfigItem("JUDICIAL_BACKGROUND", ValidationMode.PASSIVE, 4, false, "legal.pending")
                )
        ))
        .then(seedConfigIfNotExists(new EndpointValidationConfig(
                null,
                BREB_VINCULACION_PROCESS_CODE,
                List.of(
                        new EndpointValidationConfig.ConfigItem("OTP_PHONE_SMS", ValidationMode.ACTIVE, 1, true, "otp.required")
                )
        )))
        .then(seedConfigIfNotExists(new EndpointValidationConfig(
                null,
                BREB_CAMBIO_PROCESS_CODE,
                List.of(
                        new EndpointValidationConfig.ConfigItem("OTP_PHONE_SMS", ValidationMode.ACTIVE, 1, true, "otp.required")
                )
        )));
    }

    private Mono<Void> seedConfigIfNotExists(EndpointValidationConfig config) {
        return configRepo.existsByProcessCode(config.processCode())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        log.debug("Endpoint validation config for '{}' already exists, skipping", config.processCode());
                        return Mono.empty();
                    }
                    return configRepo.save(config)
                            .doOnSuccess(saved -> log.info("Seeded endpoint validation config for processCode='{}'", saved.processCode()))
                            .then();
                });
    }
}
