package com.findu.security.application.service.impl;

import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolRepositoryPort;
import com.findu.security.application.port.output.persistence.UsuarioRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.domain.model.Operation;
import com.findu.security.domain.model.Rol;
import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepoAdapter;
    @Mock
    private RolRepositoryPort rolRepositoryPort;
    @Mock
    private UserRolRepositoryPort userRolRepositoryPort;
    @Mock
    private RolOperationRepositoryPort rolOperationRepositoryPort;

    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(usuarioRepoAdapter, rolRepositoryPort, userRolRepositoryPort, rolOperationRepositoryPort);
    }

    @Test
    void getUserByUsernameWithRole_loadsAuthorities() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setUsername("a");
        Rol rol = new Rol();
        rol.setId(1);
        rol.setName("ROLE_CUSTOMER");

        Operation op = new Operation();
        op.setName("AUTH_LOGIN");

        when(usuarioRepoAdapter.getByUsername("a")).thenReturn(Mono.just(u));
        when(rolRepositoryPort.findByName("ROLE_CUSTOMER")).thenReturn(Mono.just(rol));
        when(userRolRepositoryPort.findRoleByUserId(1L)).thenReturn(Mono.just(rol));
        when(rolOperationRepositoryPort.findOperationsByRoleId(1)).thenReturn(Flux.just(op));

        StepVerifier.create(service.getUserByUsernameWithRole("a", "ROLE_CUSTOMER"))
                .expectNextMatches(user -> user.getGrantedAuthorities() != null
                        && user.getGrantedAuthorities().stream().anyMatch(a -> "AUTH_LOGIN".equals(a.getAuthority())))
                .verifyComplete();
    }

    @Test
    void getUserByUsernameWithRole_wrongRole_returnsEmpty() {
        Usuario u = new Usuario();
        u.setId(1L);
        Rol rol = new Rol();
        rol.setId(2);
        rol.setName("ROLE_CUSTOMER");
        Rol other = new Rol();
        other.setId(99);

        when(usuarioRepoAdapter.getByUsername("a")).thenReturn(Mono.just(u));
        when(rolRepositoryPort.findByName("ROLE_CUSTOMER")).thenReturn(Mono.just(rol));
        when(userRolRepositoryPort.findRoleByUserId(1L)).thenReturn(Mono.just(other));

        StepVerifier.create(service.getUserByUsernameWithRole("a", "ROLE_CUSTOMER"))
                .verifyComplete();
    }
}
