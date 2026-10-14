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
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, Long> idempotencyCache = new ConcurrentHashMap<>();

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
    public Mono<InternalUserResponse> createUser(InternalCreateUserRequest request, String idempotencyKey) {
        log.info("Internal user creation requested for username={} email={} phone={} idempotencyKey={}",
                request.username(), request.email(), request.phone(), idempotencyKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Long cachedUserId = idempotencyCache.get(idempotencyKey);
            if (cachedUserId != null) {
                log.info("Idempotent retry for idempotencyKey={}, returning cached userId={}", idempotencyKey, cachedUserId);
                return Mono.just(new InternalUserResponse(cachedUserId));
            }
        }

        String username = request.username() != null ? request.username().trim() : "";
        String email = request.email() != null ? request.email().trim().toLowerCase() : "";
        String rawPhone = request.phone() != null ? request.phone().trim() : "";
        String cleanPhone = rawPhone.replaceAll("[\\s+\\-]", "");
        String nationalPhone = cleanPhone.startsWith("57") ? cleanPhone.substring(2) : cleanPhone;
        String country = (request.codPhoneInternational() != null && !request.codPhoneInternational().isBlank())
                ? request.codPhoneInternational().trim() : "+57";

        // Unicidad estricta: username, email y teléfono deben ser únicos (Plan B)
        return usuarioService.existsByUsername(username)
                .flatMap(existsUser -> {
                    if (Boolean.TRUE.equals(existsUser)) {
                        log.warn("User creation rejected: username '{}' already taken", username);
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con estos datos, inicia sesión"));
                    }
                    return usuarioService.existsByEmail(email);
                })
                .flatMap(existsEmail -> {
                    if (Boolean.TRUE.equals(existsEmail)) {
                        log.warn("User creation rejected: email '{}' already taken", email);
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con estos datos, inicia sesión"));
                    }
                    if (!nationalPhone.isBlank()) {
                        return usuarioService.getUserByPhoneAndCountry(country, nationalPhone)
                                .map(u -> true)
                                .defaultIfEmpty(false)
                                .flatMap(existsPhoneWithCountry -> {
                                    if (Boolean.TRUE.equals(existsPhoneWithCountry)) {
                                        log.warn("User creation rejected: phone '{}{}' already taken", country, nationalPhone);
                                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con estos datos, inicia sesión"));
                                    }
                                    return usuarioService.existsByPhone(nationalPhone);
                                });
                    }
                    return Mono.just(false);
                })
                .flatMap(existsPhone -> {
                    if (Boolean.TRUE.equals(existsPhone)) {
                        log.warn("User creation rejected: phone '{}' already taken", nationalPhone);
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con estos datos, inicia sesión"));
                    }
                    return doCreateUser(request, nationalPhone, country, idempotencyKey);
                });
    }

    private Mono<InternalUserResponse> doCreateUser(InternalCreateUserRequest request, String nationalPhone, String country, String idempotencyKey) {
        Usuario usuario = new Usuario();
        usuario.setUsername(request.username().trim());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setPhone(nationalPhone);
        usuario.setCodPhoneInternational(country);
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
                                                .bind("provider", request.provider().trim().toLowerCase())
                                                .bind("providerUserId", request.providerUserId().trim())
                                                .then();
                                    }
                                    return Mono.empty();
                                }))
                                .then(Mono.fromCallable(() -> {
                                    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                                        idempotencyCache.put(idempotencyKey, savedUser.getId());
                                    }
                                    return new InternalUserResponse(savedUser.getId());
                                }))));
    }

    @Override
    public Mono<Void> deleteUser(Long userId) {
        if (userId == null) {
            return Mono.empty();
        }
        log.info("Compensating user creation by deleting userId={}", userId);
        if (!idempotencyCache.isEmpty()) {
            idempotencyCache.values().removeIf(id -> id != null && id.equals(userId));
        }
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
