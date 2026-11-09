package com.findu.core.application.service;

import com.findu.core.application.dto.request.ActualizarPerfilProveedorRequest;
import com.findu.core.application.dto.response.PerfilProveedorDetalleResponse;
import com.findu.core.domain.model.PerfilEspecialista;
import com.findu.core.domain.model.PerfilProveedor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionPerfilProveedorUseCaseImplTest {

    @Mock private PerfilProveedorService perfilProveedorService;
    @Mock private PerfilEspecialistaService perfilEspecialistaService;
    @Mock private DireccionService direccionService;
    @Mock private PortafolioService portafolioService;
    @Mock private EspecialistaCredencialService credencialService;
    @Mock private ProveedorCoberturaService coberturaService;
    @Mock private com.findu.core.application.port.out.persistence.MunicipioRepositoryPort municipioPort;
    @Mock private com.findu.core.application.port.out.persistence.ServicioRepositoryPort servicioPort;

    @InjectMocks private GestionPerfilProveedorUseCaseImpl useCase;

    private PerfilEspecialista especialidad(BigDecimal calificacion) {
        return PerfilEspecialista.builder()
                .id(1L)
                .perfilProveedorId(10L)
                .servicioId(100L)
                .descripcion("desc")
                .experienciaAnios(2)
                .calificacionPromedio(calificacion)
                .active(true)
                .build();
    }

    private void stubComun(List<PerfilEspecialista> especialidades) {
        PerfilProveedor perfil = PerfilProveedor.builder()
                .id(10L)
                .nombreCompleto("Proveedor Test")
                .calificacionPromedio(new BigDecimal("3.5"))
                .estado("ACTIVO")
                .estadoVerificacion("PENDIENTE")
                .build();
        when(perfilProveedorService.findById(10L)).thenReturn(Optional.of(perfil));
        when(perfilEspecialistaService.findByProveedorId(10L)).thenReturn(especialidades);
        when(servicioPort.findAll()).thenReturn(List.of());
        when(direccionService.findByProveedorId(10L)).thenReturn(List.of());
        when(coberturaService.findByProveedorId(10L)).thenReturn(List.of());
    }

    private PerfilProveedor perfilConEstado(String estado, String foto) {
        PerfilProveedor perfil = PerfilProveedor.builder()
                .id(10L)
                .nombreCompleto("Proveedor Test")
                .urlImagenPerfil(foto)
                .estado("ACTIVO")
                .estadoVerificacion(estado)
                .build();
        when(perfilProveedorService.findById(10L)).thenReturn(Optional.of(perfil));
        when(perfilProveedorService.save(org.mockito.ArgumentMatchers.any(PerfilProveedor.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        return perfil;
    }

    @Test
    void verificadoConNombreDistintoLanza409() {
        perfilConEstado("VERIFICADO", null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest("Otro Nombre", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Los datos verificados no se pueden modificar; abre un ticket de soporte");
        org.mockito.Mockito.verify(perfilProveedorService, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void verificadoConMismoNombreYFotoNuevaPermite() {
        PerfilProveedor perfil = perfilConEstado("VERIFICADO", null);
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest("  Proveedor Test ", "http://foto"));
        assertThat(perfil.getUrlImagenPerfil()).isEqualTo("http://foto");
        org.mockito.Mockito.verify(perfilProveedorService).save(perfil);
    }

    @Test
    void pendienteConNombreDistintoActualiza() {
        PerfilProveedor perfil = perfilConEstado("PENDIENTE", null);
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest("Otro Nombre", null));
        assertThat(perfil.getNombreCompleto()).isEqualTo("Otro Nombre");
    }

    @Test
    void verificadoSinNombreYConFotoPermite() {
        PerfilProveedor perfil = perfilConEstado("VERIFICADO", null);
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, "http://foto"));
        assertThat(perfil.getUrlImagenPerfil()).isEqualTo("http://foto");
    }

    @Test
    void verificadoNoSobrescribeFotoExistente() {
        PerfilProveedor perfil = perfilConEstado("VERIFICADO", "http://original");
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, "http://nueva"));
        assertThat(perfil.getUrlImagenPerfil()).isEqualTo("http://original");
    }

    @Test
    void promedioDeDosEspecialidades() {
        stubComun(List.of(especialidad(new BigDecimal("3.5")), especialidad(new BigDecimal("4.5"))));

        PerfilProveedorDetalleResponse resp = useCase.consultarPerfil(10L);

        assertThat(resp.calificacionPromedio()).isEqualByComparingTo(new BigDecimal("4.0"));
    }

    @Test
    void sinEspecialidadesActivasUsaFallback() {
        stubComun(List.of());

        PerfilProveedorDetalleResponse resp = useCase.consultarPerfil(10L);

        assertThat(resp.calificacionPromedio()).isEqualByComparingTo(new BigDecimal("3.5"));
    }

    @Test
    void unaSolaEspecialidad() {
        stubComun(List.of(especialidad(new BigDecimal("4.3"))));

        PerfilProveedorDetalleResponse resp = useCase.consultarPerfil(10L);

        assertThat(resp.calificacionPromedio()).isEqualByComparingTo(new BigDecimal("4.3"));
    }

    @Test
    void promedioRedondeadoHalfUp() {
        stubComun(List.of(
                especialidad(new BigDecimal("3.5")),
                especialidad(new BigDecimal("4.0")),
                especialidad(new BigDecimal("5.0"))));

        PerfilProveedorDetalleResponse resp = useCase.consultarPerfil(10L);

        // promedio 4.1666... -> HALF_UP a 1 decimal = 4.2
        assertThat(resp.calificacionPromedio()).isEqualByComparingTo(new BigDecimal("4.2"));
    }
}
