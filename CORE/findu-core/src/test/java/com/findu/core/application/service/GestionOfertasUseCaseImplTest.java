package com.findu.core.application.service;

import com.findu.core.application.dto.request.CrearOfertaRequest;
import com.findu.core.application.dto.response.OfertaResponse;
import com.findu.core.application.port.out.externalapi.NotificationPort;
import com.findu.core.application.port.out.persistence.CategoriaRepositoryPort;
import com.findu.core.application.port.out.persistence.MunicipioRepositoryPort;
import com.findu.core.application.port.out.persistence.ServicioRepositoryPort;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.domain.observer.SolicitudEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionOfertasUseCaseImplTest {

    @Mock private OfertaService ofertaService;
    @Mock private SolicitudServicioService solicitudService;
    @Mock private PerfilProveedorService proveedorService;
    @Mock private PerfilClienteService perfilClienteService;
    @Mock private DireccionService direccionService;
    @Mock private NotificationPort notificationPort;
    @Mock private SolicitudEventPublisher eventPublisher;
    @Mock private ServicioRepositoryPort servicioPort;
    @Mock private PerfilEspecialistaService perfilEspecialistaService;
    @Mock private ProveedorCoberturaService proveedorCoberturaService;
    @Mock private CategoriaRepositoryPort categoriaPort;
    @Mock private MunicipioRepositoryPort municipioPort;
    @Mock private CalificacionService calificacionService;

    @InjectMocks private GestionOfertasUseCaseImpl useCase;

    private SolicitudServicio solicitudBase;
    private PerfilProveedor proveedorBase;
    private PerfilCliente clienteBase;
    private LocalDateTime fechaOriginal;

    @BeforeEach
    void setUp() {
        fechaOriginal = LocalDate.now().plusDays(2).atTime(10, 0);

        solicitudBase = SolicitudServicio.builder()
                .id(100L)
                .perfilClienteId(200L)
                .servicioId(1L)
                .direccionId(50L)
                .fechaProgramada(fechaOriginal)
                .presupuestoMaximo(new BigDecimal("150000.00"))
                .esPresupuestoEstricto(false)
                .estadoSolicitud("ABIERTA")
                .build();

        proveedorBase = PerfilProveedor.builder()
                .id(10L)
                .nombreCompleto("Juan Proveedor")
                .urlImagenPerfil("https://findu.test/avatar.png")
                .calificacionPromedio(new BigDecimal("4.8"))
                .build();

        clienteBase = PerfilCliente.builder()
                .id(200L)
                .nombreCompleto("Maria Cliente")
                .username("mcliente")
                .build();

        when(solicitudService.findById(100L)).thenReturn(Optional.of(solicitudBase));
        when(proveedorService.findById(10L)).thenReturn(Optional.of(proveedorBase));
        when(perfilClienteService.findById(200L)).thenReturn(Optional.of(clienteBase));

        when(ofertaService.save(any(Oferta.class))).thenAnswer(invocation -> {
            Oferta o = invocation.getArgument(0);
            if (o.getId() == null) {
                o.setId(501L);
            }
            return o;
        });
    }

    @Nested
    @DisplayName("1. Contrapropuesta que modifica únicamente el precio")
    class ModificaUnicamentePrecio {

        @Test
        void debePersistirNuevoPrecioYSinHoraPropuesta() {
            CrearOfertaRequest request = new CrearOfertaRequest(
                    100L, 10L, new BigDecimal("180000.00"), null, "2 horas", "Tengo amplia experiencia"
            );

            OfertaResponse response = useCase.enviarOferta(request);

            assertThat(response).isNotNull();
            assertThat(response.valorPropuesto()).isEqualByComparingTo(new BigDecimal("180000.00"));
            assertThat(response.horaInicioPropuesta()).isNull();
            assertThat(response.estadoOferta()).isEqualTo("ENVIADA");

            ArgumentCaptor<Oferta> ofertaCaptor = ArgumentCaptor.forClass(Oferta.class);
            verify(ofertaService).save(ofertaCaptor.capture());
            assertThat(ofertaCaptor.getValue().getValorPropuesto()).isEqualByComparingTo(new BigDecimal("180000.00"));
            assertThat(ofertaCaptor.getValue().getHoraInicioPropuesta()).isNull();

            // La solicitud transiciona a EN_NEGOCIACION
            assertThat(solicitudBase.getEstadoSolicitud()).isEqualTo("EN_NEGOCIACION");
            verify(solicitudService).save(solicitudBase);
        }
    }

    @Nested
    @DisplayName("2. Contrapropuesta que modifica únicamente la hora de inicio")
    class ModificaUnicamenteHoraInicio {

        @Test
        void debePersistirNuevaHoraPropuestaYDefaultPrecioOriginalSolicitud() {
            LocalTime nuevaHora = LocalTime.of(16, 30);
            CrearOfertaRequest request = new CrearOfertaRequest(
                    100L, 10L, null, nuevaHora, "3 horas", "Puedo en la tarde"
            );

            OfertaResponse response = useCase.enviarOferta(request);

            assertThat(response).isNotNull();
            // Toma el presupuesto original de la solicitud como precio acordado si no se modificó
            assertThat(response.valorPropuesto()).isEqualByComparingTo(new BigDecimal("150000.00"));
            assertThat(response.horaInicioPropuesta()).isEqualTo(nuevaHora);

            ArgumentCaptor<Oferta> ofertaCaptor = ArgumentCaptor.forClass(Oferta.class);
            verify(ofertaService).save(ofertaCaptor.capture());
            assertThat(ofertaCaptor.getValue().getHoraInicioPropuesta()).isEqualTo(nuevaHora);
            assertThat(ofertaCaptor.getValue().getValorPropuesto()).isEqualByComparingTo(new BigDecimal("150000.00"));
        }
    }

    @Nested
    @DisplayName("3. Contrapropuesta que modifica precio y hora simultáneamente")
    class ModificaPrecioYHoraSimultaneamente {

        @Test
        void debePersistirAmbosValoresEnUnaSolaOperacion() {
            LocalTime nuevaHora = LocalTime.of(14, 0);
            BigDecimal nuevoPrecio = new BigDecimal("175000.00");
            CrearOfertaRequest request = new CrearOfertaRequest(
                    100L, 10L, nuevoPrecio, nuevaHora, "1 hora", "Puedo a las 2pm por mejor precio"
            );

            OfertaResponse response = useCase.enviarOferta(request);

            assertThat(response.valorPropuesto()).isEqualByComparingTo(nuevoPrecio);
            assertThat(response.horaInicioPropuesta()).isEqualTo(nuevaHora);

            ArgumentCaptor<Oferta> ofertaCaptor = ArgumentCaptor.forClass(Oferta.class);
            verify(ofertaService).save(ofertaCaptor.capture());
            assertThat(ofertaCaptor.getValue().getValorPropuesto()).isEqualByComparingTo(nuevoPrecio);
            assertThat(ofertaCaptor.getValue().getHoraInicioPropuesta()).isEqualTo(nuevaHora);
        }
    }

    @Nested
    @DisplayName("4. Consulta de la contrapropuesta por parte del cliente")
    class ConsultaContrapropuestaCliente {

        @Test
        void debeRetornarListaConPrecioYHoraPropuesta() {
            LocalTime hora1 = LocalTime.of(15, 0);
            Oferta oferta1 = Oferta.builder()
                    .id(1L)
                    .solicitudServicioId(100L)
                    .perfilProveedorId(10L)
                    .valorPropuesto(new BigDecimal("160000.00"))
                    .horaInicioPropuesta(hora1)
                    .tiempoEstimado("2 horas")
                    .mensajePresentacion("Mensaje 1")
                    .estadoOferta("ENVIADA")
                    .build();

            Oferta oferta2 = Oferta.builder()
                    .id(2L)
                    .solicitudServicioId(100L)
                    .perfilProveedorId(10L)
                    .valorPropuesto(new BigDecimal("140000.00"))
                    .horaInicioPropuesta(null) // Solo precio
                    .tiempoEstimado("1 hora")
                    .mensajePresentacion("Mensaje 2")
                    .estadoOferta("ENVIADA")
                    .build();

            when(ofertaService.findBySolicitudId(100L)).thenReturn(List.of(oferta1, oferta2));

            List<OfertaResponse> lista = useCase.listarOfertasPorSolicitud(100L);

            assertThat(lista).hasSize(2);
            assertThat(lista.get(0).valorPropuesto()).isEqualByComparingTo(new BigDecimal("160000.00"));
            assertThat(lista.get(0).horaInicioPropuesta()).isEqualTo(hora1);

            assertThat(lista.get(1).valorPropuesto()).isEqualByComparingTo(new BigDecimal("140000.00"));
            assertThat(lista.get(1).horaInicioPropuesta()).isNull();
        }
    }

    @Nested
    @DisplayName("5. Aceptación que actualiza correctamente precio y hora")
    class AceptacionConPrecioYHora {

        @Test
        void debeActualizarSolicitudConNuevaHoraYConservarFecha() {
            LocalTime horaPropuesta = LocalTime.of(15, 45);
            BigDecimal precioPropuesto = new BigDecimal("170000.00");

            Oferta oferta = Oferta.builder()
                    .id(50L)
                    .solicitudServicioId(100L)
                    .perfilProveedorId(10L)
                    .valorPropuesto(precioPropuesto)
                    .horaInicioPropuesta(horaPropuesta)
                    .estadoOferta("ENVIADA")
                    .build();

            solicitudBase.setEstadoSolicitud("EN_NEGOCIACION");

            when(ofertaService.findById(50L)).thenReturn(Optional.of(oferta));
            when(ofertaService.findBySolicitudId(100L)).thenReturn(List.of(oferta));

            OfertaResponse response = useCase.aceptarOferta(50L);

            assertThat(response.estadoOferta()).isEqualTo("ACEPTADA");
            assertThat(oferta.getEstadoOferta()).isEqualTo("ACEPTADA");

            // La solicitud queda ACEPTADA
            assertThat(solicitudBase.getEstadoSolicitud()).isEqualTo("ACEPTADA");
            // La fecha conserva el día original pero con la hora propuesta
            assertThat(solicitudBase.getFechaProgramada().toLocalDate()).isEqualTo(fechaOriginal.toLocalDate());
            assertThat(solicitudBase.getFechaProgramada().toLocalTime()).isEqualTo(horaPropuesta);

            verify(solicitudService).save(solicitudBase);
            verify(ofertaService).save(oferta);
        }
    }

    @Nested
    @DisplayName("6. Conservación de la hora original cuando no se propone una alternativa")
    class ConservacionHoraOriginal {

        @Test
        void debeMantenerFechaYHoraOriginalAlAceptarOfertaSinHora() {
            BigDecimal precioPropuesto = new BigDecimal("165000.00");

            Oferta oferta = Oferta.builder()
                    .id(51L)
                    .solicitudServicioId(100L)
                    .perfilProveedorId(10L)
                    .valorPropuesto(precioPropuesto)
                    .horaInicioPropuesta(null) // Sin hora alternativa
                    .estadoOferta("ENVIADA")
                    .build();

            solicitudBase.setEstadoSolicitud("EN_NEGOCIACION");

            when(ofertaService.findById(51L)).thenReturn(Optional.of(oferta));
            when(ofertaService.findBySolicitudId(100L)).thenReturn(List.of(oferta));

            OfertaResponse response = useCase.aceptarOferta(51L);

            assertThat(response.estadoOferta()).isEqualTo("ACEPTADA");
            // La fecha y hora original no se modifican
            assertThat(solicitudBase.getFechaProgramada()).isEqualTo(fechaOriginal);
        }
    }

    @Nested
    @DisplayName("7. Rechazo de datos inválidos y de operaciones sobre estados no permitidos")
    class ValidacionesYEvemtosNoPermitidos {

        @Test
        void rechazaSiNoSeEspecificaNiPrecioNiHora() {
            CrearOfertaRequest request = new CrearOfertaRequest(100L, 10L, null, null, "1h", "nada");

            assertThatThrownBy(() -> useCase.enviarOferta(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Debe proponer al menos un precio o una hora de inicio");
        }

        @Test
        void rechazaPrecioCeroONegativo() {
            CrearOfertaRequest request = new CrearOfertaRequest(
                    100L, 10L, BigDecimal.ZERO, null, "1h", "cero"
            );

            assertThatThrownBy(() -> useCase.enviarOferta(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mayor a cero");
        }

        @Test
        void rechazaOfertaQueExcedePresupuestoEstricto() {
            solicitudBase.setEsPresupuestoEstricto(true);
            solicitudBase.setPresupuestoMaximo(new BigDecimal("100000.00"));

            CrearOfertaRequest request = new CrearOfertaRequest(
                    100L, 10L, new BigDecimal("120000.00"), null, "1h", "alto"
            );

            assertThatThrownBy(() -> useCase.enviarOferta(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("tope máximo estricto");
        }

        @Test
        void rechazaHoraEnElPasadoRespectoALaFechaDelServicio() {
            // Solicitud para hoy, hora propuesta en el pasado
            solicitudBase.setFechaProgramada(LocalDateTime.now().minusHours(2));

            LocalTime horaPasada = LocalTime.now().minusHours(3);
            CrearOfertaRequest request = new CrearOfertaRequest(
                    100L, 10L, new BigDecimal("100000.00"), horaPasada, "1h", "ayer"
            );

            assertThatThrownBy(() -> useCase.enviarOferta(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pasado");
        }

        @Test
        void rechazaOfertaSiSolicitudNoEstaAbiertaNiEnNegociacion() {
            solicitudBase.setEstadoSolicitud("CANCELADA");

            CrearOfertaRequest request = new CrearOfertaRequest(
                    100L, 10L, new BigDecimal("100000.00"), null, "1h", "tarde"
            );

            assertThatThrownBy(() -> useCase.enviarOferta(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No se pueden enviar ofertas a una solicitud en estado: CANCELADA");
        }

        @Test
        void rechazaAceptarOfertaQueNoEstaEnviada() {
            Oferta oferta = Oferta.builder()
                    .id(52L)
                    .solicitudServicioId(100L)
                    .perfilProveedorId(10L)
                    .valorPropuesto(new BigDecimal("100000.00"))
                    .estadoOferta("RECHAZADA")
                    .build();

            when(ofertaService.findById(52L)).thenReturn(Optional.of(oferta));

            assertThatThrownBy(() -> useCase.aceptarOferta(52L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Solo se pueden aceptar ofertas en estado ENVIADA");
        }

        @Test
        void rechazaAceptarOfertaSiSolicitudYaEstaAceptada() {
            Oferta oferta = Oferta.builder()
                    .id(53L)
                    .solicitudServicioId(100L)
                    .perfilProveedorId(10L)
                    .valorPropuesto(new BigDecimal("100000.00"))
                    .estadoOferta("ENVIADA")
                    .build();

            solicitudBase.setEstadoSolicitud("ACEPTADA");

            when(ofertaService.findById(53L)).thenReturn(Optional.of(oferta));

            assertThatThrownBy(() -> useCase.aceptarOferta(53L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Ya existe una oferta aceptada");
        }
    }

    @Nested
    @DisplayName("8. Compatibilidad con solicitudes históricas")
    class CompatibilidadHistorica {

        @Test
        void ofertaHistoricaConHoraNullSeAceptaCorrectamente() {
            Oferta ofertaHistorica = Oferta.builder()
                    .id(99L)
                    .solicitudServicioId(100L)
                    .perfilProveedorId(10L)
                    .valorPropuesto(new BigDecimal("130000.00"))
                    .horaInicioPropuesta(null) // Campo ausente/null en base histórica
                    .estadoOferta("ENVIADA")
                    .build();

            solicitudBase.setEstadoSolicitud("EN_NEGOCIACION");

            when(ofertaService.findById(99L)).thenReturn(Optional.of(ofertaHistorica));
            when(ofertaService.findBySolicitudId(100L)).thenReturn(List.of(ofertaHistorica));

            OfertaResponse response = useCase.aceptarOferta(99L);

            assertThat(response.estadoOferta()).isEqualTo("ACEPTADA");
            assertThat(response.horaInicioPropuesta()).isNull();
            // Conserva la fecha y hora original exactamente
            assertThat(solicitudBase.getFechaProgramada()).isEqualTo(fechaOriginal);
            assertThat(solicitudBase.getEstadoSolicitud()).isEqualTo("ACEPTADA");
        }

        @Test
        void solicitudHistoricaEnEstadoAbiertaAceptaOfertaSinFallar() {
            Oferta oferta = Oferta.builder()
                    .id(98L)
                    .solicitudServicioId(100L)
                    .perfilProveedorId(10L)
                    .valorPropuesto(new BigDecimal("145000.00"))
                    .horaInicioPropuesta(LocalTime.of(11, 15))
                    .estadoOferta("ENVIADA")
                    .build();

            // La solicitud aún tiene estado ABIERTA en registros legacy
            solicitudBase.setEstadoSolicitud("ABIERTA");

            when(ofertaService.findById(98L)).thenReturn(Optional.of(oferta));
            when(ofertaService.findBySolicitudId(100L)).thenReturn(List.of(oferta));

            OfertaResponse response = useCase.aceptarOferta(98L);

            assertThat(response.estadoOferta()).isEqualTo("ACEPTADA");
            assertThat(solicitudBase.getEstadoSolicitud()).isEqualTo("ACEPTADA");
            assertThat(solicitudBase.getFechaProgramada().toLocalTime()).isEqualTo(LocalTime.of(11, 15));
        }
    }
}
