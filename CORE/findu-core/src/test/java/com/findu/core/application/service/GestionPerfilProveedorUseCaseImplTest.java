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
    @Mock private FotoUrlResolver fotoUrlResolver;

    @Mock private com.findu.core.application.port.out.storage.ObjectStoragePort storage;

    @InjectMocks private GestionPerfilProveedorUseCaseImpl useCase;

    @Test
    void evidenciaOkSoloConEspejoYRostroEnS3() {
        stubComun(List.of());
        PerfilProveedor perfil = perfilProveedorService.findById(10L).get();
        perfil.setTipoIdentificacion("CC");
        perfil.setNumeroIdentificacion("1234");
        assertThat(useCase.consultarPerfil(10L).biometriaEvidenciaOk()).isFalse();

        perfil.setBiometriaVerificadaAt(java.time.LocalDateTime.now());
        when(storage.exists("proveedores/CC1234/biometrico/rostro/rostro.png")).thenReturn(false);
        assertThat(useCase.consultarPerfil(10L).biometriaEvidenciaOk()).isFalse();

        when(storage.exists("proveedores/CC1234/biometrico/rostro/rostro.png")).thenReturn(true);
        assertThat(useCase.consultarPerfil(10L).biometriaEvidenciaOk()).isTrue();

        when(storage.exists(org.mockito.ArgumentMatchers.anyString())).thenThrow(new RuntimeException("s3 caido"));
        assertThat(useCase.consultarPerfil(10L).biometriaEvidenciaOk()).isFalse();
    }

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
                        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest("Otro Nombre", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Los datos verificados no se pueden modificar; abre un ticket de soporte");
        org.mockito.Mockito.verify(perfilProveedorService, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void verificadoConMismoNombreYFotoNuevaPermite() {
        PerfilProveedor perfil = perfilConEstado("VERIFICADO", null);
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest("  Proveedor Test ", "http://foto", null));
        assertThat(perfil.getUrlImagenPerfil()).isEqualTo("http://foto");
        org.mockito.Mockito.verify(perfilProveedorService).save(perfil);
    }

    @Test
    void pendienteConNombreDistintoActualiza() {
        PerfilProveedor perfil = perfilConEstado("PENDIENTE", null);
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest("Otro Nombre", null, null));
        assertThat(perfil.getNombreCompleto()).isEqualTo("Otro Nombre");
    }

    @Test
    void verificadoSinNombreYConFotoPermite() {
        PerfilProveedor perfil = perfilConEstado("VERIFICADO", null);
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, "http://foto", null));
        assertThat(perfil.getUrlImagenPerfil()).isEqualTo("http://foto");
    }

    @Test
    void verificadoNoSobrescribeFotoExistente() {
        PerfilProveedor perfil = perfilConEstado("VERIFICADO", "http://original");
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, "http://nueva", null));
        assertThat(perfil.getUrlImagenPerfil()).isEqualTo("http://original");
    }

    private PerfilProveedor conBiometria() {
        PerfilProveedor perfil = perfilConEstado("PENDIENTE", null);
        perfil.setFechaNacimiento(java.time.LocalDate.of(1990, 5, 20));
        perfil.setBiometriaVerificadaAt(java.time.LocalDateTime.now());
        return perfil;
    }

    @Test
    void biometriaConMismoNombreYMismaFechaPasa() {
        PerfilProveedor perfil = conBiometria();
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(" Proveedor Test", null, java.time.LocalDate.of(1990, 5, 20)));
        assertThat(perfil.getFechaNacimiento()).isEqualTo(java.time.LocalDate.of(1990, 5, 20));
        org.mockito.Mockito.verify(perfilProveedorService).save(perfil);
    }

    @Test
    void biometriaConNombreDistintoLanza409() {
        conBiometria();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest("Otro Nombre", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Los datos verificados no se pueden modificar; abre un ticket de soporte");
    }

    @Test
    void biometriaConFechaDistintaLanza409() {
        PerfilProveedor perfil = conBiometria();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, null, java.time.LocalDate.of(1991, 1, 1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Los datos verificados no se pueden modificar; abre un ticket de soporte");
        assertThat(perfil.getFechaNacimiento()).isEqualTo(java.time.LocalDate.of(1990, 5, 20));
        org.mockito.Mockito.verify(perfilProveedorService, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sinBiometriaFechaFuturaSeRechaza() {
        perfilConEstado("PENDIENTE", null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, null, java.time.LocalDate.now().plusDays(1))))
                .isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.verify(perfilProveedorService, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sinBiometriaFechaYNombrePersisten() {
        PerfilProveedor perfil = perfilConEstado("PENDIENTE", null);
        perfil.setFechaNacimiento(java.time.LocalDate.of(1990, 5, 20));
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest("Nuevo Nombre", null, java.time.LocalDate.of(1992, 2, 2)));
        assertThat(perfil.getFechaNacimiento()).isEqualTo(java.time.LocalDate.of(1992, 2, 2));
        assertThat(perfil.getNombreCompleto()).isEqualTo("Nuevo Nombre");
    }

    @Test
    void fotoClaveDelPropioProveedorSeAcepta() {
        PerfilProveedor perfil = perfilConEstado("PENDIENTE", null);
        perfil.setTipoIdentificacion("CC");
        perfil.setNumeroIdentificacion("1.234");
        String key = "proveedores/CC1234/perfil/20260101000000_abcd1234.png";
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, key, null));
        assertThat(perfil.getUrlImagenPerfil()).isEqualTo(key);
    }

    @Test
    void fotoClaveDeOtroProveedorYRutasLocalesSeIgnoran() {
        PerfilProveedor perfil = perfilConEstado("PENDIENTE", null);
        perfil.setTipoIdentificacion("CC");
        perfil.setNumeroIdentificacion("1234");
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, "proveedores/CC9999/perfil/x.png", null));
        assertThat(perfil.getUrlImagenPerfil()).isNull();
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, "/data/user/0/cache/foto.jpg", null));
        assertThat(perfil.getUrlImagenPerfil()).isNull();
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, "/findu-core/api/v1/perfil-proveedor/10/foto-perfil?v=1", null));
        assertThat(perfil.getUrlImagenPerfil()).isNull();
    }

    @Test
    void fotoBajoBiometricoSeIgnora() {
        PerfilProveedor perfil = perfilConEstado("PENDIENTE", null);
        perfil.setTipoIdentificacion("CC");
        perfil.setNumeroIdentificacion("1234");
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, "proveedores/CC1234/biometrico/documento/frente.png", null));
        assertThat(perfil.getUrlImagenPerfil()).isNull();
        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, "proveedores/CC1234/biometrico/rostro/rostro.png", null));
        assertThat(perfil.getUrlImagenPerfil()).isNull();
    }

    @Test
    void biometriaConFechaActualNulaNoPermiteFijarla() {
        PerfilProveedor perfil = conBiometria();
        perfil.setFechaNacimiento(null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest(null, null, java.time.LocalDate.of(1991, 1, 1))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(perfil.getFechaNacimiento()).isNull();
    }

    @Test
    void biometriaConNombreActualBlancoNoPermiteFijarlo() {
        PerfilProveedor perfil = conBiometria();
        perfil.setNombreCompleto(" ");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        useCase.actualizarPerfil(10L, new ActualizarPerfilProveedorRequest("Otro", null, null)))
                .isInstanceOf(IllegalStateException.class);
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
