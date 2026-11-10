package com.findu.core.application.service;

import com.findu.core.application.dto.response.ClienteDetalleResponse;
import com.findu.core.application.dto.response.ClienteResumenResponse;
import com.findu.core.application.dto.response.PaginaResponse;
import com.findu.core.application.port.in.GestionMisClientesUseCase.FotoCliente;
import com.findu.core.application.port.out.persistence.ClasificacionClienteRepositoryPort;
import com.findu.core.application.port.out.persistence.ClienteProveedorFavoritoRepositoryPort;
import com.findu.core.application.port.out.persistence.MisClientesQueryPort;
import com.findu.core.domain.enums.ChatEstado;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.ClienteProveedorFavorito;
import com.findu.core.domain.model.ClienteVinculoResumen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GestionMisClientesUseCaseImplTest {

    private static final long PROV = 1L;
    private static final UUID REF = UUID.randomUUID();

    private MisClientesQueryPort query;
    private ClienteProveedorFavoritoRepositoryPort favoritos;
    private ClasificacionClienteRepositoryPort clasificacion;
    private GestionMisClientesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        query = mock(MisClientesQueryPort.class);
        favoritos = mock(ClienteProveedorFavoritoRepositoryPort.class);
        clasificacion = mock(ClasificacionClienteRepositoryPort.class);
        useCase = new GestionMisClientesUseCaseImpl(query, favoritos, clasificacion);
    }

    private static ClienteVinculoResumen fila(boolean activo) {
        return new ClienteVinculoResumen(5L, REF, "Ana Perez", true, false, 0L, false, activo);
    }

    private void conVinculo(long clienteId, boolean activo) {
        when(favoritos.findByProveedorAndClienteRef(PROV, REF)).thenReturn(Optional.of(
                ClienteProveedorFavorito.builder().perfilClienteId(clienteId).perfilProveedorId(PROV)
                        .clienteRef(REF).activo(activo).build()));
    }

    @Test
    void conVinculoApareceConCeroServicios() {
        when(query.listar(eq(PROV), any(), any())).thenReturn(new PageImpl<>(List.of(fila(true))));
        PaginaResponse<ClienteResumenResponse> p = useCase.listar(PROV, null, 0, 20);
        assertEquals(1, p.content().size());
        assertEquals(0L, p.content().get(0).serviciosRealizados());
        assertEquals(ChatEstado.NO_DISPONIBLE_AUN, p.content().get(0).chatEstado());
    }

    @Test
    void tras_baja_sigue_en_lista_con_chat_bloqueado() {
        when(query.listar(eq(PROV), any(), any())).thenReturn(new PageImpl<>(List.of(fila(false))));
        ClienteResumenResponse r = useCase.listar(PROV, "", 0, 20).content().get(0);
        assertEquals(false, r.activo());
        assertEquals(ChatEstado.BLOQUEADO, r.chatEstado());
    }

    @Test
    void tamanoSeAcotaA50() {
        when(query.listar(eq(PROV), any(), any())).thenReturn(new PageImpl<>(List.of()));
        useCase.listar(PROV, null, -3, 5000);
        ArgumentCaptor<Pageable> c = ArgumentCaptor.forClass(Pageable.class);
        verify(query).listar(eq(PROV), any(), c.capture());
        assertEquals(50, c.getValue().getPageSize());
        assertEquals(0, c.getValue().getPageNumber());
    }

    @Test
    void detalleBajaTambienDaChatBloqueado() {
        when(query.detalle(PROV, REF)).thenReturn(Optional.of(fila(false)));
        ClienteDetalleResponse d = useCase.detalle(PROV, REF);
        assertEquals(ChatEstado.BLOQUEADO, d.chatEstado());
    }

    @Test
    void sinVinculoDetalleServiciosYFotoDan404() {
        when(query.detalle(PROV, REF)).thenReturn(Optional.empty());
        when(favoritos.findByProveedorAndClienteRef(PROV, REF)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.detalle(PROV, REF));
        assertThrows(ResourceNotFoundException.class, () -> useCase.historial(PROV, REF, 0, 20));
        assertThrows(ResourceNotFoundException.class, () -> useCase.foto(PROV, REF));
        assertThrows(ResourceNotFoundException.class, () -> useCase.definirFavoritoInterno(PROV, REF, true));
        verify(query, never()).historial(anyLong(), anyLong(), any());
        verify(query, never()).fotoCliente(anyLong());
        verifyNoInteractions(clasificacion);
    }

    @Test
    void fotoDecodificaDataUrl() {
        conVinculo(5L, true);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2};
        when(query.fotoCliente(5L)).thenReturn(Optional.of("data:image/png;base64," + Base64.getEncoder().encodeToString(png)));
        FotoCliente f = useCase.foto(PROV, REF);
        assertEquals("image/png", f.contentType());
        assertEquals(png.length, f.bytes().length);
    }

    @Test
    void fotoQueNoEsBase64Da404() {
        conVinculo(5L, true);
        when(query.fotoCliente(5L)).thenReturn(Optional.of("https://cdn.example/foto.jpg"));
        assertThrows(ResourceNotFoundException.class, () -> useCase.foto(PROV, REF));
        when(query.fotoCliente(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.foto(PROV, REF));
    }

    @Test
    void favoritoInternoSoloTocaClasificacionYFuncionaConVinculoInactivo() {
        conVinculo(5L, false);
        useCase.definirFavoritoInterno(PROV, REF, true);
        verify(clasificacion).guardarFavoritoInterno(PROV, 5L, true);
        verify(favoritos, never()).save(any());
    }
}