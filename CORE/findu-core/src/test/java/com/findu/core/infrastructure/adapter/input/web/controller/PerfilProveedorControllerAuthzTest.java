package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.port.in.GestionPerfilProveedorUseCase;
import com.findu.core.infrastructure.security.AuthzProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.findu.core.infrastructure.adapter.input.web.controller.AuthzMockMvcSupport.INTERNAL_KEY;
import static com.findu.core.infrastructure.adapter.input.web.controller.AuthzMockMvcSupport.bearer;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PerfilProveedorControllerAuthzTest {

    private GestionPerfilProveedorUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(GestionPerfilProveedorUseCase.class);
        AuthzProperties props = AuthzMockMvcSupport.enforceProps();
        mvc = AuthzMockMvcSupport.mvc(props,
                new PerfilProveedorController(useCase, AuthzMockMvcSupport.guard(props)));
    }

    @Test
    void propietarioConsultaSuPerfil() throws Exception {
        mvc.perform(get("/api/v1/perfil-proveedor/1").header("Authorization", bearer(2)))
                .andExpect(status().isOk());
        verify(useCase).consultarPerfil(1L);
    }

    @Test
    void otroProveedorRecibe403SinTocarElCasoDeUso() throws Exception {
        mvc.perform(get("/api/v1/perfil-proveedor/1").header("Authorization", bearer(9)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/perfil-proveedor/1").header("Authorization", bearer(9))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/perfil-proveedor/1/estado").header("Authorization", bearer(9))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/perfil-proveedor/1/cobertura").header("Authorization", bearer(9)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/perfil-proveedor/1/cobertura").header("Authorization", bearer(9))
                        .contentType(MediaType.APPLICATION_JSON).content("[\"05001\"]"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/perfil-proveedor/1/disponibilidad").header("Authorization", bearer(9))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"disponible\":true}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mvc.perform(get("/api/v1/perfil-proveedor/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/perfil-proveedor/auth/2")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/perfil-proveedor/1").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    void authUserIdSeComparaDirectoConElClaim() throws Exception {
        mvc.perform(get("/api/v1/perfil-proveedor/auth/2").header("Authorization", bearer(2)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/perfil-proveedor/usuario/2").header("Authorization", bearer(2)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/perfil-proveedor/auth/2").header("Authorization", bearer(9)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/perfil-proveedor/usuario/2").header("Authorization", bearer(9)))
                .andExpect(status().isForbidden());
    }

    @Test
    void propietarioActualizaCoberturaYDisponibilidad() throws Exception {
        mvc.perform(put("/api/v1/perfil-proveedor/1/cobertura").header("Authorization", bearer(2))
                        .contentType(MediaType.APPLICATION_JSON).content("[\"05001\"]"))
                .andExpect(status().isNoContent());
        verify(useCase).actualizarCobertura(1L, List.of("05001"));
        mvc.perform(patch("/api/v1/perfil-proveedor/1/disponibilidad").header("Authorization", bearer(2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"disponible\":false}"))
                .andExpect(status().isOk());
        verify(useCase).actualizarDisponibilidad(1L, false);
    }

    @Test
    void llaveInternaValidaPasaComoServicioAServicio() throws Exception {
        mvc.perform(get("/api/v1/perfil-proveedor/auth/2").header(JwtHeader.KEY, INTERNAL_KEY))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/perfil-proveedor/auth/2").header(JwtHeader.KEY, "otra"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicoNoPasaPorElGuard() throws Exception {
        mvc.perform(get("/api/v1/perfil-proveedor/1/publico").param("servicioId", "3"))
                .andExpect(status().isOk());
        verify(useCase, never()).consultarPerfil(anyLong());
    }

    private static final class JwtHeader {
        static final String KEY = "X-Internal-Service-Key";
    }
}
