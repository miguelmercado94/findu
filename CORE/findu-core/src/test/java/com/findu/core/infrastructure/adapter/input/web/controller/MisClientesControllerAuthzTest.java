package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.dto.response.ClienteDetalleResponse;
import com.findu.core.application.dto.response.ClienteResumenResponse;
import com.findu.core.application.dto.response.PaginaResponse;
import com.findu.core.application.dto.response.ServicioHistorialItemResponse;
import com.findu.core.application.port.in.GestionMisClientesUseCase;
import com.findu.core.application.port.in.GestionMisClientesUseCase.FotoCliente;
import com.findu.core.domain.enums.ChatEstado;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilClienteRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import com.findu.core.infrastructure.security.AuthzMode;
import com.findu.core.infrastructure.security.CurrentActorResolver;
import com.findu.core.infrastructure.security.JwtVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.findu.core.infrastructure.adapter.input.web.controller.AuthzMockMvcSupport.bearer;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proveedor A: usuario 2 -> perfil 1. Proveedor B: usuario 4 -> perfil 2. Usuario 3: token valido sin perfil proveedor.
 * Mismo comportamiento con authz.mode OFF, LOG y ENFORCE (el endpoint no depende del JwtContextFilter).
 */
class MisClientesControllerAuthzTest {

    private static final String BASE = "/api/v1/proveedor/mis-clientes";
    private static final UUID REF = UUID.randomUUID();

    private GestionMisClientesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = mock(GestionMisClientesUseCase.class);
        when(useCase.listar(anyLong(), any(), anyInt(), anyInt())).thenReturn(new PaginaResponse<>(
                List.of(new ClienteResumenResponse(REF, "Ana Perez", false, false, 0, false, true,
                        ChatEstado.NO_DISPONIBLE_AUN)), 0, 20, 1L, 1));
        when(useCase.detalle(1L, REF)).thenReturn(new ClienteDetalleResponse(REF, "Ana Perez", true, true, 3, false,
                false, ChatEstado.BLOQUEADO));
        when(useCase.detalle(2L, REF)).thenThrow(new ResourceNotFoundException("Cliente", REF));
        when(useCase.historial(1L, REF, 0, 20)).thenReturn(new PaginaResponse<>(
                List.of(new ServicioHistorialItemResponse("Plomeria", "Hogar", LocalDateTime.of(2026, 1, 2, 3, 4), "FINALIZADA")),
                0, 20, 1L, 1));
        when(useCase.foto(1L, REF)).thenReturn(new FotoCliente(new byte[]{1, 2, 3}, "image/png"));
        when(useCase.foto(2L, REF)).thenThrow(new ResourceNotFoundException("Foto", REF));
    }

    private MockMvc mvc(AuthzMode mode) {
        PerfilProveedorRepository proveedores = mock(PerfilProveedorRepository.class);
        when(proveedores.findByAuthUserId(2L)).thenReturn(Optional.of(perfil(1L, 2L)));
        when(proveedores.findByAuthUserId(4L)).thenReturn(Optional.of(perfil(2L, 4L)));
        CurrentActorResolver resolver = new CurrentActorResolver(new JwtVerifier(AuthzMockMvcSupport.SECRET),
                proveedores, mock(PerfilClienteRepository.class));
        return AuthzMockMvcSupport.mvc(AuthzMockMvcSupport.propsFor(mode), new MisClientesController(useCase, resolver));
    }

    private static PerfilProveedorEntity perfil(long id, long authUserId) {
        PerfilProveedorEntity p = new PerfilProveedorEntity();
        p.setId(id);
        p.setAuthUserId(authUserId);
        return p;
    }

    private static final Set<String> RUTAS = Set.of("", "/" + REF, "/" + REF + "/servicios", "/" + REF + "/foto");

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void sinTokenDa401EnTodosLosEndpoints(AuthzMode mode) throws Exception {
        MockMvc mvc = mvc(mode);
        for (String ruta : RUTAS) {
            mvc.perform(get(BASE + ruta)).andExpect(status().isUnauthorized());
        }
        mvc.perform(put(BASE + "/" + REF + "/favorito-interno").contentType(MediaType.APPLICATION_JSON)
                .content("{\"favorito\":true}")).andExpect(status().isUnauthorized());
        verify(useCase, never()).definirFavoritoInterno(anyLong(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void tokenInvalidoDa401(AuthzMode mode) throws Exception {
        mvc(mode).perform(get(BASE).header("Authorization", "Bearer basura")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void usuarioSinPerfilProveedorDa403(AuthzMode mode) throws Exception {
        MockMvc mvc = mvc(mode);
        for (String ruta : RUTAS) {
            mvc.perform(get(BASE + ruta).header("Authorization", bearer(3))).andExpect(status().isForbidden());
        }
        mvc.perform(put(BASE + "/" + REF + "/favorito-interno").header("Authorization", bearer(3))
                .contentType(MediaType.APPLICATION_JSON).content("{\"favorito\":true}")).andExpect(status().isForbidden());
        verify(useCase, never()).listar(anyLong(), any(), anyInt(), anyInt());
    }

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void llaveInternaNoSustituyeAlJwt(AuthzMode mode) throws Exception {
        mvc(mode).perform(get(BASE).header("X-Internal-Service-Key", AuthzMockMvcSupport.INTERNAL_KEY))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void proveedorBNoVeClienteDeAComoNoExistente(AuthzMode mode) throws Exception {
        MockMvc mvc = mvc(mode);
        mvc.perform(get(BASE + "/" + REF).header("Authorization", bearer(4))).andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/" + REF + "/foto").header("Authorization", bearer(4))).andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/" + REF).header("Authorization", bearer(2))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void elProveedorSaleSoloDelJwtYNoDeParametros(AuthzMode mode) throws Exception {
        mvc(mode).perform(get(BASE + "?proveedorId=2&perfilProveedorId=2").header("Authorization", bearer(2)))
                .andExpect(status().isOk());
        verify(useCase).listar(1L, null, 0, 20);
        verify(useCase, never()).listar(2L, null, 0, 20);
    }

    @Test
    void ningunMetodoDelControllerRecibeIdDeProveedor() {
        for (Method m : MisClientesController.class.getDeclaredMethods()) {
            String params = Arrays.stream(m.getParameters()).map(p -> p.getName().toLowerCase())
                    .collect(Collectors.joining(","));
            assertFalse(params.contains("proveedor"), m.getName() + " recibe parametro de proveedor: " + params);
        }
    }

    @Test
    void respuestasSoloContienenClavesDeLaWhitelist() throws Exception {
        MockMvc mvc = mvc(AuthzMode.OFF);
        String lista = mvc.perform(get(BASE).header("Authorization", bearer(2))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].clienteRef").value(REF.toString()))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get(BASE + "/" + REF).header("Authorization", bearer(2))).andExpect(status().isOk())
                .andExpect(jsonPath("$.chatEstado").value("BLOQUEADO"))
                .andExpect(jsonPath("$.serviciosRealizados").value(3));
        String historial = mvc.perform(get(BASE + "/" + REF + "/servicios").header("Authorization", bearer(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].estado").value("FINALIZADA"))
                .andReturn().getResponse().getContentAsString();
        for (String respuesta : List.of(lista, historial)) {
            String l = respuesta.toLowerCase();
            assertFalse(l.contains("ticket") || l.contains("telefono") || l.contains("email") || l.contains("direccion")
                    || l.contains("codigo") || l.contains("\"id\""), respuesta);
        }
    }

    @Test
    void fotoDevuelveBytesConCachePrivado() throws Exception {
        mvc(AuthzMode.OFF).perform(get(BASE + "/" + REF + "/foto").header("Authorization", bearer(2)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @ParameterizedTest
    @EnumSource(AuthzMode.class)
    void favoritoInternoResponde204Idempotente(AuthzMode mode) throws Exception {
        MockMvc mvc = mvc(mode);
        for (int i = 0; i < 2; i++) {
            mvc.perform(put(BASE + "/" + REF + "/favorito-interno").header("Authorization", bearer(2))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"favorito\":true}"))
                    .andExpect(status().isNoContent());
        }
        verify(useCase, org.mockito.Mockito.times(2)).definirFavoritoInterno(1L, REF, true);
    }

    @Test
    void favoritoInternoSinCampoDa400() throws Exception {
        mvc(AuthzMode.OFF).perform(put(BASE + "/" + REF + "/favorito-interno").header("Authorization", bearer(2))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    void favoritoInternoDeClienteSinVinculoDa404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Cliente", REF)).when(useCase)
                .definirFavoritoInterno(2L, REF, true);
        mvc(AuthzMode.OFF).perform(put(BASE + "/" + REF + "/favorito-interno").header("Authorization", bearer(4))
                .contentType(MediaType.APPLICATION_JSON).content("{\"favorito\":true}")).andExpect(status().isNotFound());
    }
}