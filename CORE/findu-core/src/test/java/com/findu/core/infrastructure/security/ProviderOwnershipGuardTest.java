package com.findu.core.infrastructure.security;

import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProviderOwnershipGuardTest {

    private final PerfilProveedorRepository repo = mock(PerfilProveedorRepository.class);
    private final AuthzProperties props = new AuthzProperties();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/findu-core/api/v1/perfil-proveedor/1");
    private final ProviderOwnershipGuard guard = new ProviderOwnershipGuard(props, repo, request);

    @BeforeEach
    void setUp() {
        PerfilProveedorEntity perfil = new PerfilProveedorEntity();
        perfil.setAuthUserId(2L);
        when(repo.findById(1L)).thenReturn(Optional.of(perfil));
        when(repo.findById(99L)).thenReturn(Optional.empty());
    }

    private void principal(long userId) {
        request.setAttribute(JwtContextFilter.ATTR_PRINCIPAL, new AuthenticatedPrincipal(userId, "u", "ROLE_OUR_PROVEEDOR"));
    }

    private void assertStatus(Runnable r, HttpStatus status) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(status.value()));
    }

    @Test
    void offNoHaceNadaNiConsultaBd() {
        props.setMode(AuthzMode.OFF);
        assertThatCode(() -> guard.requirePerfil("perfil", 1L)).doesNotThrowAnyException();
        assertThatCode(() -> guard.requireAuthUser("perfil", 2L)).doesNotThrowAnyException();
        verifyNoInteractions(repo);
    }

    @Test
    void logNuncaBloquea() {
        props.setMode(AuthzMode.LOG);
        assertThatCode(() -> guard.requirePerfil("perfil", 1L)).doesNotThrowAnyException(); // sin token
        principal(5);
        assertThatCode(() -> guard.requirePerfil("perfil", 1L)).doesNotThrowAnyException(); // otro
        assertThatCode(() -> guard.requireAuthUser("perfil", 2L)).doesNotThrowAnyException();
    }

    @Test
    void enforcePropietarioPasa() {
        props.setMode(AuthzMode.ENFORCE);
        principal(2);
        assertThatCode(() -> guard.requirePerfil("perfil", 1L)).doesNotThrowAnyException();
        assertThatCode(() -> guard.requireAuthUser("perfil", 2L)).doesNotThrowAnyException();
    }

    @Test
    void enforceOtroProveedorEs403() {
        props.setMode(AuthzMode.ENFORCE);
        principal(5);
        assertStatus(() -> guard.requirePerfil("perfil", 1L), HttpStatus.FORBIDDEN);
        assertStatus(() -> guard.requireAuthUser("perfil", 2L), HttpStatus.FORBIDDEN);
    }

    @Test
    void enforceSinTokenEs401SinConsultarBd() {
        props.setMode(AuthzMode.ENFORCE);
        assertStatus(() -> guard.requirePerfil("perfil", 1L), HttpStatus.UNAUTHORIZED);
        assertStatus(() -> guard.requireAuthUser("perfil", 2L), HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(repo);
    }

    @Test
    void enforceTokenInvalidoEs401() {
        props.setMode(AuthzMode.ENFORCE);
        request.setAttribute(JwtContextFilter.ATTR_TOKEN_STATE, JwtContextFilter.BAD_TOKEN);
        assertStatus(() -> guard.requirePerfil("perfil", 1L), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void enforcePerfilInexistenteConTokenValidoPasaParaQueResponda404() {
        props.setMode(AuthzMode.ENFORCE);
        principal(5);
        assertThatCode(() -> guard.requirePerfil("perfil", 99L)).doesNotThrowAnyException();
    }

    @Test
    void enforceSoloBloqueaAmbitosListados() {
        props.setMode(AuthzMode.ENFORCE);
        props.setEnforceScopes(List.of("archivos"));
        principal(5);
        assertThatCode(() -> guard.requirePerfil("perfil", 1L)).doesNotThrowAnyException(); // solo LOG
        assertStatus(() -> guard.requirePerfil("archivos", 1L), HttpStatus.FORBIDDEN);
    }

    @Test
    void servicioInternoConLlaveValidaPasa() {
        props.setMode(AuthzMode.ENFORCE);
        request.setAttribute(JwtContextFilter.ATTR_INTERNAL_OK, Boolean.TRUE);
        assertThatCode(() -> guard.requirePerfil("perfil", 1L)).doesNotThrowAnyException();
        assertThatCode(() -> guard.requireAuthUser("perfil", 2L)).doesNotThrowAnyException();
        verifyNoInteractions(repo);
    }
}
