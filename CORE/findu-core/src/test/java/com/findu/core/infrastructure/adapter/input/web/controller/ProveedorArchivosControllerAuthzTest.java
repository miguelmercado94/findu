package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.port.in.GestionArchivosProveedorUseCase;
import com.findu.core.application.port.in.GestionArchivosProveedorUseCase.FotoPerfil;
import com.findu.core.infrastructure.security.AuthzProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static com.findu.core.infrastructure.adapter.input.web.controller.AuthzMockMvcSupport.bearer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProveedorArchivosControllerAuthzTest {

    private static final String URL = "/api/v1/perfil-proveedor/1/foto-perfil";
    private static final String JSON = "{\"imagenBase64\":\"aGVsbG8=\"}";

    private GestionArchivosProveedorUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(GestionArchivosProveedorUseCase.class);
        AuthzProperties props = AuthzMockMvcSupport.enforceProps();
        mvc = AuthzMockMvcSupport.mvc(props,
                new ProveedorArchivosController(useCase, AuthzMockMvcSupport.guard(props)));
    }

    @Test
    void propietarioCambiaFotoPorJsonYMultipart() throws Exception {
        when(useCase.cambiarFotoPerfil(anyLong(), any())).thenReturn("s3://foto");
        mvc.perform(put(URL).header("Authorization", bearer(2)).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, URL)
                        .file(new MockMultipartFile("file", "f.jpg", "image/jpeg", new byte[]{1, 2}))
                        .header("Authorization", bearer(2)))
                .andExpect(status().isOk());
    }

    @Test
    void otroProveedorRecibe403() throws Exception {
        mvc.perform(put(URL).header("Authorization", bearer(9)).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isForbidden());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, URL)
                        .file(new MockMultipartFile("file", "f.jpg", "image/jpeg", new byte[]{1, 2}))
                        .header("Authorization", bearer(9)))
                .andExpect(status().isForbidden());
        verify(useCase, never()).cambiarFotoPerfil(anyLong(), any());
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    void getFotoPerfilSigueSiendoPublico() throws Exception {
        when(useCase.obtenerFotoPerfil(1L)).thenReturn(new FotoPerfil(new byte[]{1}, "image/jpeg", "k"));
        mvc.perform(get(URL)).andExpect(status().isOk());
    }
}
