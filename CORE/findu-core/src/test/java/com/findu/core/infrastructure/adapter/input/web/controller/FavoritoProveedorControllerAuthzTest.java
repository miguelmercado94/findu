package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.port.in.GestionFavoritosClienteUseCase;
import com.findu.core.application.port.in.GestionFavoritosClienteUseCase.FavoritoProveedor;
import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilClienteEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilClienteRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import com.findu.core.infrastructure.security.AuthzMode;
import com.findu.core.infrastructure.security.CurrentActorResolver;
import com.findu.core.infrastructure.security.JwtVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.findu.core.infrastructure.adapter.input.web.controller.AuthzMockMvcSupport.bearer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cliente 5 pertenece al usuario 7. Debe comportarse igual con authz.mode OFF, LOG y ENFORCE. */
class FavoritoProveedorControllerAuthzTest {

    private static final String BASE = "/api/v1/perfil-cliente/5/favoritos";

    private GestionFavoritosClienteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = mock(GestionFavoritosClienteUseCase.class);
    }

    private MockMvc mvc(AuthzMode mode) {
        PerfilClienteRepository clientes = mock(PerfilClienteRepository.class);
        PerfilClienteEntity cliente = new PerfilClienteEntity();
        cliente.setId(5L);
        cliente.setAuthUserId(7L);
        when(clientes.findById(5L)).thenReturn(Optional.of(cliente));
        CurrentActorResolver resolver = new CurrentActorResolver(new JwtVerifier(AuthzMockMvcSupport.SECRET),
                mock(PerfilProveedorRepository.class), clientes);
        return AuthzMockMvcSupport.mvc(AuthzMockMvcSupport.propsFor(mode),
                new FavoritoProveedorController(useCase, resolver));
    }

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void sinTokenDa401(AuthzMode mode) throws Exception {
        MockMvc mvc = mvc(mode);
        mvc.perform(put(BASE + "/10")).andExpect(status().isUnauthorized());
        mvc.perform(delete(BASE + "/10")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        verify(useCase, never()).marcarFavorito(5L, 10L);
        verify(useCase, never()).quitarFavorito(5L, 10L);
    }

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void clienteAjenoDa403(AuthzMode mode) throws Exception {
        MockMvc mvc = mvc(mode);
        mvc.perform(put(BASE + "/10").header("Authorization", bearer(8))).andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/10").header("Authorization", bearer(8))).andExpect(status().isForbidden());
        mvc.perform(get(BASE).header("Authorization", bearer(8))).andExpect(status().isForbidden());
        verify(useCase, never()).marcarFavorito(5L, 10L);
        verify(useCase, never()).quitarFavorito(5L, 10L);
    }

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void duenoMarcaYQuitaConDelegacionAlUseCase(AuthzMode mode) throws Exception {
        MockMvc mvc = mvc(mode);
        mvc.perform(put(BASE + "/10").header("Authorization", bearer(7))).andExpect(status().isNoContent());
        mvc.perform(delete(BASE + "/10").header("Authorization", bearer(7))).andExpect(status().isNoContent());
        verify(useCase).marcarFavorito(5L, 10L);
        verify(useCase).quitarFavorito(5L, 10L);
    }

    @Test
    void duenoListaSusFavoritos() throws Exception {
        when(useCase.listar(5L)).thenReturn(List.of(new FavoritoProveedor(10L, true), new FavoritoProveedor(11L, false)));
        mvc(AuthzMode.OFF).perform(get(BASE).header("Authorization", bearer(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].proveedorId").value(10))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$[1].activo").value(false));
    }
}