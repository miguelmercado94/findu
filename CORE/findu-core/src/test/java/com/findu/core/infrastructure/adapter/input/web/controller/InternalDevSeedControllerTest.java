package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.port.out.persistence.ClienteProveedorFavoritoRepositoryPort;
import com.findu.core.application.port.out.persistence.PerfilClienteRepositoryPort;
import com.findu.core.application.port.out.persistence.PerfilProveedorRepositoryPort;
import com.findu.core.domain.model.ClienteProveedorFavorito;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.infrastructure.adapter.input.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalDevSeedControllerTest {

    private static final String URL = "/internal/dev/mis-clientes/vinculo";
    private static final String KEY = "llave-de-prueba";
    private static final String BODY = "{\"perfilClienteId\":1,\"perfilProveedorId\":2,\"activo\":false}";

    private ClienteProveedorFavoritoRepositoryPort favoritos;
    private InternalDevSeedController controller;

    @BeforeEach
    void setUp() {
        favoritos = mock(ClienteProveedorFavoritoRepositoryPort.class);
        PerfilClienteRepositoryPort clientes = mock(PerfilClienteRepositoryPort.class);
        PerfilProveedorRepositoryPort proveedores = mock(PerfilProveedorRepositoryPort.class);
        when(clientes.findById(1L)).thenReturn(Optional.of(new PerfilCliente()));
        when(proveedores.findById(2L)).thenReturn(Optional.of(new PerfilProveedor()));
        when(favoritos.findByClienteAndProveedor(1L, 2L)).thenReturn(Optional.empty());
        when(favoritos.save(any())).thenAnswer(i -> {
            ClienteProveedorFavorito f = i.getArgument(0);
            f.setId(9L);
            return f;
        });
        controller = new InternalDevSeedController(favoritos, clientes, proveedores);
        ReflectionTestUtils.setField(controller, "expectedInternalKey", KEY);
    }

    private MockMvc mvc(boolean enabled) {
        ReflectionTestUtils.setField(controller, "enabled", enabled);
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void conFlagApagadoResponde404AunConLlaveValida() throws Exception {
        mvc(false).perform(post(URL).header("X-Internal-Service-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isNotFound());
        verify(favoritos, never()).save(any());
    }

    @Test
    void sinLlaveDa401YLlaveIncorrectaDa403() throws Exception {
        MockMvc mvc = mvc(true);
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(post(URL).header("X-Internal-Service-Key", "otra").contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isForbidden());
        verify(favoritos, never()).save(any());
    }

    @Test
    void conFlagYLlaveCreaVinculoDadoDeBaja() throws Exception {
        mvc(true).perform(post(URL).header("X-Internal-Service-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.clienteRef").isNotEmpty());
        verify(favoritos).save(any());
    }

    @Test
    void perfilInexistenteDa404() throws Exception {
        mvc(true).perform(post(URL).header("X-Internal-Service-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                .content("{\"perfilClienteId\":77,\"perfilProveedorId\":2}")).andExpect(status().isNotFound());
    }
}