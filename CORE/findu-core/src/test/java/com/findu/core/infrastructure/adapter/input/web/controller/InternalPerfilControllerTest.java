package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.dto.request.internal.InternalCrearPerfilRequest;
import com.findu.core.application.dto.request.internal.InternalPersonaRequest;
import com.findu.core.application.dto.response.internal.InternalPerfilResponse;
import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.DireccionRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.EspecialistaCredencialRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilClienteRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilEspecialistaRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PortafolioItemRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.ProveedorCoberturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalPerfilControllerTest {

    @Mock private PerfilProveedorRepository proveedorRepository;
    @Mock private PerfilClienteRepository clienteRepository;
    @Mock private DireccionRepository direccionRepository;
    @Mock private ProveedorCoberturaRepository coberturaRepository;
    @Mock private PerfilEspecialistaRepository especialistaRepository;
    @Mock private PortafolioItemRepository portafolioRepository;
    @Mock private EspecialistaCredencialRepository credencialRepository;

    @InjectMocks
    private InternalPerfilController controller;

    private static final String SERVICE_KEY = "test-internal-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "expectedInternalKey", SERVICE_KEY);
    }

    @Test
    void crearPerfil_proveedorSuccess() {
        InternalPersonaRequest persona = new InternalPersonaRequest("Juan Perez", "CC", "12345678", "1990-01-01", "M");
        InternalCrearPerfilRequest req = new InternalCrearPerfilRequest(
                10L, "PROVEEDOR", "juanp", "juan@test.com", "3001234567", "+57", persona, null);

        when(proveedorRepository.findByAuthUserId(10L)).thenReturn(Optional.empty());
        when(proveedorRepository.existsByTipoIdentificacionAndNumeroIdentificacion("CC", "12345678")).thenReturn(false);
        when(clienteRepository.existsByTipoIdentificacionAndNumeroIdentificacion("CC", "12345678")).thenReturn(false);

        PerfilProveedorEntity saved = PerfilProveedorEntity.builder().id(99L).build();
        when(proveedorRepository.save(any(PerfilProveedorEntity.class))).thenReturn(saved);

        ResponseEntity<InternalPerfilResponse> response = controller.crearPerfil(SERVICE_KEY, "idem-1", req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().perfilId()).isEqualTo(99L);
    }

    @Test
    void crearPerfil_failsWhenDocumentAlreadyExists() {
        InternalPersonaRequest persona = new InternalPersonaRequest("Juan Perez", "CC", "12345678", "1990-01-01", "M");
        InternalCrearPerfilRequest req = new InternalCrearPerfilRequest(
                10L, "PROVEEDOR", "juanp", "juan@test.com", "3001234567", "+57", persona, null);

        when(proveedorRepository.findByAuthUserId(10L)).thenReturn(Optional.empty());
        when(proveedorRepository.existsByTipoIdentificacionAndNumeroIdentificacion("CC", "12345678")).thenReturn(true);

        assertThatThrownBy(() -> controller.crearPerfil(SERVICE_KEY, "idem-2", req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("Ya existe una cuenta con estos datos, inicia sesión");
                });

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void crearPerfil_idempotencyCachedReturnsSameId() {
        InternalPersonaRequest persona = new InternalPersonaRequest("Juan Perez", "CC", "12345678", "1990-01-01", "M");
        InternalCrearPerfilRequest req = new InternalCrearPerfilRequest(
                10L, "PROVEEDOR", "juanp", "juan@test.com", "3001234567", "+57", persona, null);

        when(proveedorRepository.findByAuthUserId(10L)).thenReturn(Optional.empty());
        when(proveedorRepository.existsByTipoIdentificacionAndNumeroIdentificacion("CC", "12345678")).thenReturn(false);
        when(clienteRepository.existsByTipoIdentificacionAndNumeroIdentificacion("CC", "12345678")).thenReturn(false);

        PerfilProveedorEntity saved = PerfilProveedorEntity.builder().id(99L).build();
        when(proveedorRepository.save(any(PerfilProveedorEntity.class))).thenReturn(saved);

        ResponseEntity<InternalPerfilResponse> res1 = controller.crearPerfil(SERVICE_KEY, "idem-same", req);
        assertThat(res1.getBody().perfilId()).isEqualTo(99L);

        // Second call with same idempotency key should hit cache
        ResponseEntity<InternalPerfilResponse> res2 = controller.crearPerfil(SERVICE_KEY, "idem-same", req);
        assertThat(res2.getBody().perfilId()).isEqualTo(99L);
    }
}
