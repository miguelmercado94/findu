package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.JwtManager;
import com.findu.security.domain.model.Rol;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.internal.InternalCreateUserRequest;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalUserManagerImplTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private RolRepositoryPort rolRepositoryPort;
    @Mock
    private UserRolRepositoryPort userRolRepositoryPort;
    @Mock
    private RolOperationRepositoryPort rolOperationRepositoryPort;
    @Mock
    private ProviderActivationRepositoryPort activationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtManager jwtManager;
    @Mock
    private ConnectionFactory connectionFactory;

    private InternalUserManagerImpl internalUserManager;

    @BeforeEach
    void setUp() {
        ConnectionFactoryMetadata metadata = mock(ConnectionFactoryMetadata.class);
        when(connectionFactory.getMetadata()).thenReturn(metadata);
        when(metadata.getName()).thenReturn("PostgreSQL");

        internalUserManager = new InternalUserManagerImpl(
                usuarioService,
                rolRepositoryPort,
                userRolRepositoryPort,
                rolOperationRepositoryPort,
                activationRepository,
                passwordEncoder,
                jwtManager,
                connectionFactory
        );
    }

    @Test
    void createUser_duplicateUsername_rejectsWithConflict() {
        InternalCreateUserRequest req = new InternalCreateUserRequest(
                "existing_user", "new@test.com", "pass", "3001234567", "+57",
                "ROLE_OUR_PROVEEDOR", false, null, null
        );

        when(usuarioService.existsByUsername("existing_user")).thenReturn(Mono.just(true));

        StepVerifier.create(internalUserManager.createUser(req, "key-1"))
                .expectErrorMatches(err -> err instanceof ResponseStatusException rse
                        && rse.getStatusCode().value() == HttpStatus.CONFLICT.value()
                        && rse.getReason().contains("Ya existe una cuenta con estos datos"))
                .verify();
    }

    @Test
    void createUser_duplicateEmail_rejectsWithConflict() {
        InternalCreateUserRequest req = new InternalCreateUserRequest(
                "new_user", "existing@test.com", "pass", "3001234567", "+57",
                "ROLE_OUR_PROVEEDOR", false, null, null
        );

        when(usuarioService.existsByUsername("new_user")).thenReturn(Mono.just(false));
        when(usuarioService.existsByEmail("existing@test.com")).thenReturn(Mono.just(true));

        StepVerifier.create(internalUserManager.createUser(req, "key-2"))
                .expectErrorMatches(err -> err instanceof ResponseStatusException rse
                        && rse.getStatusCode().value() == HttpStatus.CONFLICT.value()
                        && rse.getReason().contains("Ya existe una cuenta con estos datos"))
                .verify();
    }

    @Test
    void createUser_duplicatePhone_rejectsWithConflict() {
        InternalCreateUserRequest req = new InternalCreateUserRequest(
                "new_user", "new@test.com", "pass", "3001234567", "+57",
                "ROLE_OUR_PROVEEDOR", false, null, null
        );

        when(usuarioService.existsByUsername("new_user")).thenReturn(Mono.just(false));
        when(usuarioService.existsByEmail("new@test.com")).thenReturn(Mono.just(false));
        when(usuarioService.getUserByPhoneAndCountry("+57", "3001234567")).thenReturn(Mono.just(new Usuario()));

        StepVerifier.create(internalUserManager.createUser(req, "key-3"))
                .expectErrorMatches(err -> err instanceof ResponseStatusException rse
                        && rse.getStatusCode().value() == HttpStatus.CONFLICT.value()
                        && rse.getReason().contains("Ya existe una cuenta con estos datos"))
                .verify();
    }

    @Test
    void createUser_idempotencyKeyRetry_returnsCachedUserIdWithoutRecreating() {
        InternalCreateUserRequest req = new InternalCreateUserRequest(
                "john_doe", "john@test.com", "pass", "3001234567", "+57",
                "ROLE_OUR_PROVEEDOR", false, null, null
        );

        Usuario saved = new Usuario();
        saved.setId(99L);
        saved.setUsername("john_doe");

        Rol rol = new Rol();
        rol.setId(2);
        rol.setName("ROLE_OUR_PROVEEDOR");

        when(usuarioService.existsByUsername("john_doe")).thenReturn(Mono.just(false));
        when(usuarioService.existsByEmail("john@test.com")).thenReturn(Mono.just(false));
        when(usuarioService.getUserByPhoneAndCountry("+57", "3001234567")).thenReturn(Mono.empty());
        when(usuarioService.existsByPhone("3001234567")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(usuarioService.save(any(Usuario.class))).thenReturn(Mono.just(saved));
        when(rolRepositoryPort.findByName("ROLE_OUR_PROVEEDOR")).thenReturn(Mono.just(rol));
        when(userRolRepositoryPort.assignRoleToUser(99L, 2)).thenReturn(Mono.empty());

        // First attempt creates and caches key-abc
        StepVerifier.create(internalUserManager.createUser(req, "key-abc"))
                .expectNextMatches(res -> res.userId().equals(99L))
                .verifyComplete();

        // Second attempt with same key-abc returns cached 99L without calling exists checks again
        StepVerifier.create(internalUserManager.createUser(req, "key-abc"))
                .expectNextMatches(res -> res.userId().equals(99L))
                .verifyComplete();
    }
}
