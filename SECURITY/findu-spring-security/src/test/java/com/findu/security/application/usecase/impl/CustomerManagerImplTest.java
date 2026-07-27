package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.JwtManager;
import com.findu.security.domain.model.Rol;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.request.UserRegisterDto;
import com.findu.security.dto.response.AuthToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerManagerImplTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private RolRepositoryPort rolRepositoryPort;
    @Mock
    private UserRolRepositoryPort userRolRepositoryPort;
    @Mock
    private RolOperationRepositoryPort rolOperationRepositoryPort;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtManager jwtManager;

    private CustomerManagerImpl manager;

    @BeforeEach
    void setUp() {
        manager = new CustomerManagerImpl(usuarioService, rolRepositoryPort, userRolRepositoryPort,
                rolOperationRepositoryPort, passwordEncoder, jwtManager);
    }

    @Test
    void registerNewCustomer_success() {
        var dto = new UserRegisterDto("u", "u@e.com", "1", "+57", "pwd", "ROLE_CUSTOMER");
        Usuario saved = new Usuario();
        saved.setId(10L);
        saved.setUsername("u");
        saved.setEmail("u@e.com");

        Rol rol = new Rol();
        rol.setId(1);
        rol.setName("ROLE_CUSTOMER");

        when(usuarioService.existsByUsername("u")).thenReturn(Mono.just(false));
        when(usuarioService.existsByEmail("u@e.com")).thenReturn(Mono.just(false));
        when(usuarioService.existsByPhone("1")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("pwd")).thenReturn("hash");
        when(usuarioService.save(any(Usuario.class))).thenReturn(Mono.just(saved));
        when(rolRepositoryPort.findByName("ROLE_CUSTOMER")).thenReturn(Mono.just(rol));
        when(userRolRepositoryPort.assignRoleToUser(10L, 1)).thenReturn(Mono.empty());
        when(rolOperationRepositoryPort.findOperationsByRoleId(1)).thenReturn(Flux.empty());
        when(jwtManager.buildTokensForUser(any(Usuario.class), eq("HS256")))
                .thenReturn(Mono.just(new AuthToken("a", "r", true)));

        StepVerifier.create(manager.registerNewCustomer(dto, "HS256"))
                .expectNextMatches(r -> "a".equals(r.jwt()))
                .verifyComplete();
    }

    @Test
    void registerNewCustomer_duplicateUsername_errors() {
        var dto = new UserRegisterDto("u", "u@e.com", "1", "+57", "pwd", "ROLE_CUSTOMER");
        when(usuarioService.existsByUsername("u")).thenReturn(Mono.just(true));
        when(usuarioService.existsByEmail("u@e.com")).thenReturn(Mono.just(false));
        when(usuarioService.existsByPhone("1")).thenReturn(Mono.just(false));

        StepVerifier.create(manager.registerNewCustomer(dto, "HS256"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
