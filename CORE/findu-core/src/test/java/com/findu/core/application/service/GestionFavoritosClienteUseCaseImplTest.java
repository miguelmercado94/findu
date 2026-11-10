package com.findu.core.application.service;

import com.findu.core.application.port.out.persistence.ClienteProveedorFavoritoRepositoryPort;
import com.findu.core.application.port.out.persistence.PerfilProveedorRepositoryPort;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.ClienteProveedorFavorito;
import com.findu.core.domain.model.PerfilProveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionFavoritosClienteUseCaseImplTest {

    private ClienteProveedorFavoritoRepositoryPort favoritos;
    private PerfilProveedorRepositoryPort proveedores;
    private GestionFavoritosClienteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        favoritos = mock(ClienteProveedorFavoritoRepositoryPort.class);
        proveedores = mock(PerfilProveedorRepositoryPort.class);
        useCase = new GestionFavoritosClienteUseCaseImpl(favoritos, proveedores);
        when(proveedores.findById(10L)).thenReturn(Optional.of(new PerfilProveedor()));
    }

    @Test
    void marcarCreaVinculoNuevoConRefAleatorio() {
        when(favoritos.findByClienteAndProveedor(1L, 10L)).thenReturn(Optional.empty());
        useCase.marcarFavorito(1L, 10L);
        ArgumentCaptor<ClienteProveedorFavorito> c = ArgumentCaptor.forClass(ClienteProveedorFavorito.class);
        verify(favoritos).save(c.capture());
        assertTrue(c.getValue().isActivo());
        assertNotNull(c.getValue().getClienteRef());
        assertNotNull(c.getValue().getFavoritoDesde());
    }

    @Test
    void marcarSobreVinculoActivoEsIdempotente() {
        when(favoritos.findByClienteAndProveedor(1L, 10L)).thenReturn(Optional.of(vinculo(true)));
        useCase.marcarFavorito(1L, 10L);
        useCase.marcarFavorito(1L, 10L);
        verify(favoritos, never()).save(any());
    }

    @Test
    void marcarReactivaVinculoDadoDeBajaConservandoRef() {
        ClienteProveedorFavorito baja = vinculo(false);
        baja.setRemovidoAt(LocalDateTime.now());
        UUID ref = baja.getClienteRef();
        when(favoritos.findByClienteAndProveedor(1L, 10L)).thenReturn(Optional.of(baja));
        useCase.marcarFavorito(1L, 10L);
        verify(favoritos).save(baja);
        assertTrue(baja.isActivo());
        assertNull(baja.getRemovidoAt());
        assertTrue(ref.equals(baja.getClienteRef()));
    }

    @Test
    void marcarProveedorInexistenteDa404() {
        assertThrows(ResourceNotFoundException.class, () -> useCase.marcarFavorito(1L, 99L));
        verify(favoritos, never()).save(any());
    }

    @Test
    void quitarHaceBajaLogicaYEsIdempotente() {
        ClienteProveedorFavorito activo = vinculo(true);
        when(favoritos.findByClienteAndProveedor(1L, 10L)).thenReturn(Optional.of(activo));
        useCase.quitarFavorito(1L, 10L);
        assertFalse(activo.isActivo());
        assertNotNull(activo.getRemovidoAt());
        useCase.quitarFavorito(1L, 10L);
        verify(favoritos, times(1)).save(activo);
    }

    @Test
    void quitarSinVinculoNoHaceNadaYNuncaBorra() {
        when(favoritos.findByClienteAndProveedor(1L, 10L)).thenReturn(Optional.empty());
        useCase.quitarFavorito(1L, 10L);
        verify(favoritos, never()).save(any());
    }

    @Test
    void obtenerSinVinculoDa404() {
        when(favoritos.findByClienteAndProveedor(1L, 10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.obtener(1L, 10L));
    }

    private static ClienteProveedorFavorito vinculo(boolean activo) {
        return ClienteProveedorFavorito.builder().id(1L).perfilClienteId(1L).perfilProveedorId(10L)
                .clienteRef(UUID.randomUUID()).activo(activo).build();
    }
}