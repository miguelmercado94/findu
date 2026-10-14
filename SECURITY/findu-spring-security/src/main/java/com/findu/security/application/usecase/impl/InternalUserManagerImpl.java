package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.InternalUserManager;
import com.findu.security.application.usecase.JwtManager;
import com.findu.security.domain.model.Usuario;
import com.findu.security.domain.model.ValidationState;
import com.findu.security.domain.model.ValidationType;
import com.findu.security.dto.internal.InternalActivationInitRequest;
import com.findu.security.dto.internal.InternalActivationInitResponse;
import com.findu.security.dto.internal.InternalCreateUserRequest;
import com.findu.security.dto.internal.InternalTokensResponse;
import com.findu.security.dto.internal.InternalUserResponse;
import com.findu.security.exception.ResourceNotFoundException;
import com.findu.security.util.ReactiveUserAuthoritiesLoader;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class InternalUserManagerImpl implements InternalUserManager {

    private static final Logger log = LoggerFactory.getLogger(InternalUserManagerImpl.class);

    private final UsuarioService usuarioService;
    private final RolRepositoryPort rolRepositoryPort;
    private final UserRolRepositoryPort userRolRepositoryPort;
    private final RolOperationRepositoryPort rolOperationRepositoryPort;
    private final ProviderActivationRepositoryPort activationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtManager jwtManager;
    private final DatabaseClient databaseClient;

    public InternalUserManagerImpl(UsuarioService usuarioService,
                                   RolRepositoryPort rolRepositoryPort,
                                   UserRolRepositoryPort userRolRepositoryPort,
                                   RolOperationRepositoryPort rolOperationRepositoryPort,
                                   ProviderActivationRepositoryPort activationRepository,
                                   PasswordEncoder passwordEncoder,
                                   JwtManager jwtManager,
                                   ConnectionFactory connectionFactory) {
        this.usuarioService = usuarioService;
        this.rolRepositoryPort = rolRepositoryPort;
        this.userRolRepositoryPort = userRolRepositoryPort;
        this.rolOperationRepositoryPort = rolOperationRepositoryPort;
        this.activationRepository = activationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtManager = jwtManager;
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<InternalUserResponse> createUser(InternalCreateUserRequest request) {
        log.info("Internal user creation requested for username={} email={}", request.username(), request.email());
        return usuarioService.getUserByUsername(request.username())
                .switchIfEmpty(Mono.defer(() -> usuarioService.getUserByEmail(request.email())))
                .map(existing -> {
                    log.info("User already exists with id={}, returning existing id (idempotency)", existing.getId());
                    return new InternalUserResponse(existing.getId());
                })
                .switchIfEmpty(Mono.defer(() -> doCreateUser(request)));
    }

    private Mono<InternalUserResponse> doCreateUser(InternalCreateUserRequest request) {
        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setEmail(request.email());
        usuario.setPhone(request.phone());
        usuario.setCodPhoneInternational(request.codPhoneInternational() != null ? request.codPhoneInternational() : "+57");
        String password = (request.password() != null && !request.password().isBlank())
                ? request.password() : UUID.randomUUID().toString();
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setEstado("INCOMPLETO");
        usuario.setActive(true);
        usuario.setPhoneVerified(true);
        boolean federated = Boolean.TRUE.equals(request.isFederated());
        usuario.setEmailVerified(federated);
        usuario.setAccountStatus(federated ? "PENDING_BIOMETRIC_VERIFICATION" : "PENDING_EMAIL_VERIFICATION");
        usuario.setEstadoActivacion("PENDIENTE_VALIDACIONES");

        String roleName = (request.roleName() != null && !request.roleName().isBlank())
                ? request.roleName() : "ROLE_OUR_PROVEEDOR";

        return usuarioService.save(usuario)
                .flatMap(savedUser -> rolRepositoryPort.findByName(roleName)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rol no encontrado: " + roleName)))
                        .flatMap(rol -> userRolRepositoryPort.assignRoleToUser(savedUser.getId(), rol.getId())
                                .then(Mono.defer(() -> {
                                    if (federated && request.provider() != null && request.providerUserId() != null) {
                                        return databaseClient.sql("INSERT INTO user_identity_provider (user_id, provider_name, provider_user_id) VALUES (:userId, :provider, :providerUserId)")
                                                .bind("userId", savedUser.getId())
                                                .bind("provider", request.provider())
                                                .bind("providerUserId", request.providerUserId())
                                                .then();
                                    }
                                    return Mono.empty();
                                }))
                                .thenReturn(new InternalUserResponse(savedUser.getId()))));
    }

    @Override
    public Mono<Void> deleteUser(Long userId) {
        if (userId == null) {
            return Mono.empty();
        }
        log.info("Compensating user creation by deleting userId={}", userId);
        return databaseClient.sql("DELETE FROM provider_biometric_capture WHERE user_id = :userId")
                .bind("userId", userId).then()
                .then(databaseClient.sql("DELETE FROM provider_activation_validation WHERE user_id = :userId")
                        .bind("userId", userId).then())
                .then(databaseClient.sql("DELETE FROM user_rol WHERE user_id = :userId")
                        .bind("userId", userId).then())
                .then(databaseClient.sql("DELETE FROM user_identity_provider WHERE user_id = :userId")
                        .bind("userId", userId).then())
                .then(databaseClient.sql("DELETE FROM password_recovery_token WHERE user_id = :userId")
                        .bind("userId", userId).then())
                .then(databaseClient.sql("DELETE FROM password_recovery_code WHERE user_id = :userId")
                        .bind("userId", userId).then())
                .then(databaseClient.sql("DELETE FROM email_verification_token WHERE user_id = :userId")
                        .bind("userId", userId).then())
                .then(databaseClient.sql("DELETE FROM \"user\" WHERE id = :userId")
                        .bind("userId", userId).then())
                .doOnSuccess(v -> log.info("Successfully deleted user and related entities for userId={}", userId));
    }

    @Override
    public Mono<InternalTokensResponse> issueTokens(Long userId, String algorithm) {
        String algo = (algorithm != null && !algorithm.isBlank()) ? algorithm : "HS256";
        return usuarioService.findById(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Usuario no encontrado: " + userId)))
                .flatMap(user -> userRolRepositoryPort.findRoleByUserId(userId)
                        .map(rol -> {
                            user.setRol(rol);
                            return user;
                        })
                        .flatMap(userWithRol -> ReactiveUserAuthoritiesLoader.loadAuthoritiesFromDb(rolOperationRepositoryPort, userWithRol))
                        .flatMap(u -> jwtManager.buildTokensForUser(u, algo))
                        .map(tokens -> InternalTokensResponse.of(tokens.jwt(), tokens.jwtRefresh())));
    }

    @Override
    public Mono<InternalActivationInitResponse> initActivation(InternalActivationInitRequest request) {
        return usuarioService.findById(request.userId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Usuario no encontrado: " + request.userId())))
                .flatMap(user -> {
                    boolean bioOk = Boolean.TRUE.equals(request.biometriaAprobada());
                    ValidationState bioState = bioOk ? ValidationState.OK : ValidationState.PENDIENTE;
                    ValidationState emailState = user.isEmailVerified() ? ValidationState.OK : ValidationState.PENDIENTE;

                    LocalDate fechaNac = null;
                    if (request.cedulaFechaNacimiento() != null && !request.cedulaFechaNacimiento().isBlank()) {
                        try {
                            fechaNac = LocalDate.parse(request.cedulaFechaNacimiento());
                        } catch (Exception ignored) {}
                    }
                    BigDecimal score = request.faceMatchScore() != null ? BigDecimal.valueOf(request.faceMatchScore()) : null;

                    Mono<Void> bioCaptureMono = Boolean.TRUE.equals(request.biometriaIntentada())
                            ? activationRepository.insertBiometricCapture(
                                    user.getId(),
                                    request.cedulaNombre(),
                                    fechaNac,
                                    score,
                                    request.fotoFrontalUrl(),
                                    bioOk ? ValidationState.OK : ValidationState.RECHAZADA).then()
                            : Mono.empty();

                    return activationRepository.upsertValidation(user.getId(), ValidationType.OTP, ValidationState.OK, null)
                            .then(activationRepository.upsertValidation(user.getId(), ValidationType.EMAIL, emailState, null))
                            .then(activationRepository.upsertValidation(user.getId(), ValidationType.BIOMETRIA, bioState, null))
                            .then(bioCaptureMono)
                            .then(activationRepository.updateUserActivationState(user.getId(), "PENDIENTE_VALIDACIONES", false, null))
                            .thenReturn(new InternalActivationInitResponse("PENDIENTE_VALIDACIONES", true, user.isEmailVerified(), bioOk));
                });
    }
}
