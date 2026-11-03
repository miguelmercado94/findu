package com.findu.core.domain.observer;

import com.findu.core.application.port.out.externalapi.NotificationPort;
import com.findu.core.application.port.out.persistence.CategoriaRepositoryPort;
import com.findu.core.application.port.out.persistence.MunicipioRepositoryPort;
import com.findu.core.application.port.out.persistence.ServicioRepositoryPort;
import com.findu.core.application.service.DireccionService;
import com.findu.core.application.service.PerfilClienteService;
import com.findu.core.application.service.PerfilProveedorService;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DispatcherNotificationObserverTest {

    @Mock private NotificationPort notificationPort;
    @Mock private ServicioRepositoryPort servicioPort;
    @Mock private CategoriaRepositoryPort categoriaPort;
    @Mock private DireccionService direccionService;
    @Mock private MunicipioRepositoryPort municipioPort;
    @Mock private PerfilClienteService perfilClienteService;
    @Mock private PerfilProveedorService proveedorService;

    @InjectMocks private DispatcherNotificationObserver observer;

    @Test
    @DisplayName("sendToProvider emite el recipient canónico una sola vez y añade canonicalRecipient/aliasRecipient cuando authUserId != proveedorId (CA4)")
    void emiteUnaVezYAgregaAliasCuandoAuthUserIdDifiere() {
        PerfilProveedor proveedor = PerfilProveedor.builder()
                .id(10L)
                .authUserId(99L)
                .build();

        SolicitudServicio solicitud = SolicitudServicio.builder()
                .id(500L)
                .build();

        observer.onServicioIniciado(solicitud, null, proveedor);

        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationPort, times(1)).send(
                eq("SERVICE_UPDATE"),
                eq("PROVEEDOR_10"),
                eq("SERVICIO_EN_CURSO"),
                eq("es"),
                paramsCaptor.capture());

        Map<String, String> params = paramsCaptor.getValue();
        assertThat(params).containsEntry("canonicalRecipient", "PROVEEDOR_10");
        assertThat(params).containsEntry("aliasRecipient", "PROVEEDOR_99");
    }

    @Test
    @DisplayName("sendToProvider emite una sola vez y NO añade campos de alias cuando authUserId == proveedorId")
    void emiteUnaVezSinAliasCuandoAuthUserIdIgualAProveedorId() {
        PerfilProveedor proveedor = PerfilProveedor.builder()
                .id(10L)
                .authUserId(10L)
                .build();

        SolicitudServicio solicitud = SolicitudServicio.builder()
                .id(500L)
                .build();

        observer.onServicioIniciado(solicitud, null, proveedor);

        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationPort, times(1)).send(
                eq("SERVICE_UPDATE"),
                eq("PROVEEDOR_10"),
                any(),
                eq("es"),
                paramsCaptor.capture());

        Map<String, String> params = paramsCaptor.getValue();
        assertThat(params).doesNotContainKey("canonicalRecipient");
        assertThat(params).doesNotContainKey("aliasRecipient");
    }
}
