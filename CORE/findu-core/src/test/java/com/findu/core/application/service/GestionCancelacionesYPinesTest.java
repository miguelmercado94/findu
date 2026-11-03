package com.findu.core.application.service;

import com.findu.core.application.dto.request.AdjuntarEvidenciaCancelacionRequest;
import com.findu.core.application.dto.request.IniciarServicioRequest;
import com.findu.core.application.dto.request.ResolverCancelacionSoporteRequest;
import com.findu.core.application.dto.request.SolicitarCancelacionProveedorRequest;
import com.findu.core.application.dto.response.CancelacionProveedorResponse;
import com.findu.core.application.dto.response.HistorialDisciplinarioProveedorResponse;
import com.findu.core.application.dto.response.PrevisualizacionCancelacionResponse;
import com.findu.core.application.dto.response.SolicitudResponse;
import com.findu.core.application.port.in.GestionOfertasUseCase;
import com.findu.core.application.port.out.externalapi.NotificationPort;
import com.findu.core.application.port.out.persistence.CategoriaRepositoryPort;
import com.findu.core.application.port.out.persistence.MunicipioRepositoryPort;
import com.findu.core.application.port.out.persistence.ServicioRepositoryPort;
import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import com.findu.core.domain.enums.NivelAmonestacion;
import com.findu.core.domain.model.Direccion;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.domain.observer.SolicitudEventPublisher;
import com.findu.core.infrastructure.adapter.out.externalapi.HelpServiceClient;
import com.findu.core.infrastructure.adapter.out.externalapi.TransactionServiceClient;
import com.findu.core.infrastructure.adapter.out.persistence.entity.AmonestacionProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.CancelacionProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.AmonestacionProveedorRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.CancelacionProveedorRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.SolicitudEstadoAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionCancelacionesYPinesTest {

    @Mock private CancelacionProveedorRepository cancelacionRepository;
    @Mock private AmonestacionProveedorRepository amonestacionRepository;
    @Mock private SolicitudEstadoAuditoriaRepository auditoriaRepository;
    @Mock private SolicitudServicioService solicitudService;
    @Mock private OfertaService ofertaService;
    @Mock private GestionOfertasUseCase ofertasUseCase;
    @Mock private PerfilProveedorService proveedorService;
    @Mock private PerfilClienteService clienteService;
    @Mock private DireccionService direccionService;
    @Mock private ServicioRepositoryPort servicioPort;
    @Mock private CategoriaRepositoryPort categoriaPort;
    @Mock private MunicipioRepositoryPort municipioPort;
    @Mock private HelpServiceClient helpServiceClient;
    @Mock private TransactionServiceClient transactionServiceClient;
    @Mock private NotificationPort notificationPort;
    @Mock private SolicitudEventPublisher eventPublisher;

    private PinSeguridadHelper pinSeguridadHelperDev;
    private PinSeguridadHelper pinSeguridadHelperPdn;
    private SolicitudEstadoAuditoriaService auditoriaService;
    private GestionCancelacionesProveedorUseCaseImpl cancelacionesUseCase;
    private GestionSolicitudesUseCaseImpl solicitudesUseCase;

    private SolicitudServicio solicitudAceptada;
    private Oferta ofertaAceptada;
    private PerfilProveedor proveedor;
    private PerfilCliente cliente;
    private Direccion direccionServicio;

    @BeforeEach
    void setUp() {
        pinSeguridadHelperDev = new PinSeguridadHelper("dev", "ChambiiPinSecretKey2026AESGCM32B!");
        pinSeguridadHelperPdn = new PinSeguridadHelper("pdn", "ChambiiPinSecretKey2026AESGCM32B!");
        auditoriaService = new SolicitudEstadoAuditoriaService(auditoriaRepository);

        cancelacionesUseCase = new GestionCancelacionesProveedorUseCaseImpl(
                cancelacionRepository,
                amonestacionRepository,
                solicitudService,
                ofertaService,
                proveedorService,
                clienteService,
                auditoriaService,
                helpServiceClient,
                notificationPort
        );

        solicitudesUseCase = new GestionSolicitudesUseCaseImpl(
                solicitudService,
                proveedorService,
                clienteService,
                direccionService,
                ofertaService,
                ofertasUseCase,
                cancelacionesUseCase,
                pinSeguridadHelperDev,
                auditoriaService,
                eventPublisher,
                servicioPort,
                categoriaPort,
                municipioPort,
                transactionServiceClient
        );

        proveedor = PerfilProveedor.builder()
                .id(10L)
                .authUserId(1010L)
                .nombreCompleto("Carlos Proveedor")
                .celular("3001112233")
                .estado("ACTIVO")
                .build();

        cliente = PerfilCliente.builder()
                .id(200L)
                .authUserId(2020L)
                .username("ana_cliente")
                .nombreCompleto("Ana Cliente")
                .email("ana@cliente.test")
                .celular("3109998877")
                .build();

        // Coordenadas base en Medellín: 6.2442030, -75.5812119
        direccionServicio = Direccion.builder()
                .id(50L)
                .direccionTexto("Calle 10 # 40-20")
                .municipioId(1L)
                .latitud(new BigDecimal("6.2442030"))
                .longitud(new BigDecimal("-75.5812119"))
                .build();

        String encryptedDevPin = pinSeguridadHelperDev.encriptarPin("1234");
        solicitudAceptada = SolicitudServicio.builder()
                .id(100L)
                .perfilClienteId(200L)
                .servicioId(1L)
                .direccionId(50L)
                .fechaProgramada(LocalDateTime.now().plusMinutes(45))
                .pinInicioEncrypted(encryptedDevPin)
                .recordatorio1hEnviado(false)
                .estadoSolicitud("ACEPTADA")
                .build();

        ofertaAceptada = Oferta.builder()
                .id(500L)
                .solicitudServicioId(100L)
                .perfilProveedorId(10L)
                .valorPropuesto(new BigDecimal("95000.00"))
                .estadoOferta("ACEPTADA")
                .build();

        when(solicitudService.findById(100L)).thenReturn(Optional.of(solicitudAceptada));
        when(solicitudService.save(any(SolicitudServicio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ofertaService.findBySolicitudId(100L)).thenReturn(List.of(ofertaAceptada));
        when(ofertaService.findById(500L)).thenReturn(Optional.of(ofertaAceptada));
        when(ofertaService.save(any(Oferta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proveedorService.findById(10L)).thenReturn(Optional.of(proveedor));
        when(clienteService.findById(200L)).thenReturn(Optional.of(cliente));
        when(direccionService.findById(50L)).thenReturn(Optional.of(direccionServicio));
        when(helpServiceClient.crearTicketCancelacion(anyLong(), anyString(), anyLong(), any(), anyString(), any()))
                .thenReturn("TKT-2026-0001");
        when(cancelacionRepository.save(any(CancelacionProveedorEntity.class))).thenAnswer(inv -> {
            CancelacionProveedorEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(900L);
            if (e.getCreatedAt() == null) e.setCreatedAt(LocalDateTime.now());
            return e;
        });
        when(amonestacionRepository.save(any(AmonestacionProveedorEntity.class))).thenAnswer(inv -> {
            AmonestacionProveedorEntity a = inv.getArgument(0);
            if (a.getId() == null) a.setId(700L);
            if (a.getCreatedAt() == null) a.setCreatedAt(LocalDateTime.now());
            return a;
        });
    }

    @Nested
    @DisplayName("1. Flujo de Cancelaciones Categoría 1 — Motivos relacionados con el cliente")
    class Categoria1Tests {

        @Test
        @DisplayName("Categoría 1: Crea ticket en soporte, deja solicitud en PENDIENTE_REVISION_SOPORTE y NO genera amonestación")
        void categoria1_dejaPendienteSoporteSinLiberarServicio() {
            SolicitarCancelacionProveedorRequest req = new SolicitarCancelacionProveedorRequest(
                    100L,
                    10L,
                    CategoriaCancelacionProveedor.CATEGORIA_1_CLIENTE,
                    "El cliente no responde",
                    "El cliente no abre la puerta ni contesta llamadas",
                    null,
                    null,
                    null
            );

            CancelacionProveedorResponse res = cancelacionesUseCase.solicitarCancelacion(req);

            assertThat(res.estadoCancelacion()).isEqualTo("PENDIENTE_REVISION_SOPORTE");
            assertThat(res.estadoTicket()).isEqualTo("PENDIENTE_REVISION");
            assertThat(res.ticketCodigo()).isEqualTo("TKT-2026-0001");
            assertThat(res.amonestacionAsociada()).isNull();
            assertThat(solicitudAceptada.getEstadoSolicitud()).isEqualTo("PENDIENTE_REVISION_SOPORTE");
            verify(amonestacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Categoría 1: Cuando soporte aprueba, cancela definitivamente sin amonestación y notifica al cliente")
        void categoria1_soporteAprueba_cancelaSinAmonestacion() {
            CancelacionProveedorEntity entity = CancelacionProveedorEntity.builder()
                    .id(901L)
                    .solicitudServicioId(100L)
                    .ofertaId(500L)
                    .perfilProveedorId(10L)
                    .categoria(CategoriaCancelacionProveedor.CATEGORIA_1_CLIENTE)
                    .motivo("El cliente cambia la ubicación")
                    .ticketCodigo("TKT-2026-0001")
                    .estadoCancelacion("PENDIENTE_REVISION_SOPORTE")
                    .estadoTicket("PENDIENTE_REVISION")
                    .esInjustificada(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            solicitudAceptada.setEstadoSolicitud("PENDIENTE_REVISION_SOPORTE");
            when(cancelacionRepository.findById(901L)).thenReturn(Optional.of(entity));

            CancelacionProveedorResponse res = cancelacionesUseCase.resolverPorSoporte(901L,
                    new ResolverCancelacionSoporteRequest(1L, true, "Validado por llamada con el cliente"));

            assertThat(res.decisionSoporte()).isEqualTo("APROBADO");
            assertThat(res.estadoCancelacion()).isEqualTo("CANCELADO");
            assertThat(solicitudAceptada.getEstadoSolicitud()).isEqualTo("CANCELADO");
            verify(amonestacionRepository, never()).save(any());
            verify(notificationPort, atLeastOnce()).send(eq("SERVICE_UPDATE"), anyString(), anyString(), eq("es"), anyMap());
        }

        @Test
        @DisplayName("Categoría 1: Cuando soporte rechaza, regresa la solicitud a ACEPTADA y notifica justificación al proveedor")
        void categoria1_soporteRechaza_restauraServicioAceptado() {
            CancelacionProveedorEntity entity = CancelacionProveedorEntity.builder()
                    .id(902L)
                    .solicitudServicioId(100L)
                    .ofertaId(500L)
                    .perfilProveedorId(10L)
                    .categoria(CategoriaCancelacionProveedor.CATEGORIA_1_CLIENTE)
                    .motivo("El cliente no responde")
                    .ticketCodigo("TKT-2026-0002")
                    .estadoCancelacion("PENDIENTE_REVISION_SOPORTE")
                    .estadoTicket("PENDIENTE_REVISION")
                    .esInjustificada(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            solicitudAceptada.setEstadoSolicitud("PENDIENTE_REVISION_SOPORTE");
            when(cancelacionRepository.findById(902L)).thenReturn(Optional.of(entity));

            CancelacionProveedorResponse res = cancelacionesUseCase.resolverPorSoporte(902L,
                    new ResolverCancelacionSoporteRequest(1L, false, "El cliente sí se encontraba en el domicilio"));

            assertThat(res.decisionSoporte()).isEqualTo("RECHAZADO");
            assertThat(solicitudAceptada.getEstadoSolicitud()).isEqualTo("ACEPTADA");
        }
    }

    @Nested
    @DisplayName("2. Flujo de Cancelaciones Categoría 2 — Fuerza mayor o emergencias")
    class Categoria2Tests {

        @Test
        @DisplayName("Categoría 2: Libera servicio, crea ticket con plazo de 3 días, permite adjuntar evidencia y aprobar sin amonestación")
        void categoria2_evidenciaValida_cierraSinAmonestacion_yClientePuedeReabrir() {
            SolicitarCancelacionProveedorRequest req = new SolicitarCancelacionProveedorRequest(
                    100L,
                    10L,
                    CategoriaCancelacionProveedor.CATEGORIA_2_FUERZA_MAYOR,
                    "Emergencia médica o enfermedad",
                    "Incapacidad médica de urgencia",
                    null,
                    null,
                    null
            );

            CancelacionProveedorResponse creada = cancelacionesUseCase.solicitarCancelacion(req);

            assertThat(creada.estadoTicket()).isEqualTo("PENDIENTE_EVIDENCIA");
            assertThat(creada.fechaLimiteEvidencia()).isAfter(LocalDateTime.now().plusDays(2));
            assertThat(solicitudAceptada.getEstadoSolicitud()).isEqualTo("CANCELADO");

            // Proveedor adjunta evidencia posteriormente desde su ticket
            CancelacionProveedorEntity guardada = CancelacionProveedorEntity.builder()
                    .id(900L)
                    .solicitudServicioId(100L)
                    .ofertaId(500L)
                    .perfilProveedorId(10L)
                    .categoria(CategoriaCancelacionProveedor.CATEGORIA_2_FUERZA_MAYOR)
                    .motivo("Emergencia médica o enfermedad")
                    .ticketCodigo("TKT-2026-0001")
                    .estadoCancelacion("CANCELADO")
                    .estadoTicket("PENDIENTE_EVIDENCIA")
                    .fechaLimiteEvidencia(LocalDateTime.now().plusDays(3))
                    .amonestacionGenerada(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            when(cancelacionRepository.findById(900L)).thenReturn(Optional.of(guardada));

            cancelacionesUseCase.adjuntarEvidencias(900L, new AdjuntarEvidenciaCancelacionRequest(
                    10L,
                    List.of("https://s3.chambii.com/incapacidad.pdf"),
                    "Adjunto incapacidad EPS"
            ));

            assertThat(guardada.getEstadoTicket()).isEqualTo("PENDIENTE_REVISION");

            // Soporte aprueba evidencia
            CancelacionProveedorResponse resuelta = cancelacionesUseCase.resolverPorSoporte(900L,
                    new ResolverCancelacionSoporteRequest(1L, true, "Incapacidad médica verificada"));

            assertThat(resuelta.decisionSoporte()).isEqualTo("APROBADO");
            verify(amonestacionRepository, never()).save(any());

            // El cliente decide reabrir la solicitud para buscar otro proveedor
            SolicitudResponse reabierta = solicitudesUseCase.reabrirSolicitudCliente(100L, null, null);
            assertThat(reabierta.estadoSolicitud()).isEqualTo("ABIERTA");
        }

        @Test
        @DisplayName("Categoría 2: Vencimiento de 3 días sin soporte válido genera amonestación GRAVE (7 días) sin duplicados")
        void categoria2_vencimiento3DiasSinSoporte_generaAmonestacionGraveIdempotente() {
            CancelacionProveedorEntity vencida = CancelacionProveedorEntity.builder()
                    .id(910L)
                    .solicitudServicioId(100L)
                    .ofertaId(500L)
                    .perfilProveedorId(10L)
                    .categoria(CategoriaCancelacionProveedor.CATEGORIA_2_FUERZA_MAYOR)
                    .motivo("Accidente")
                    .ticketCodigo("TKT-2026-0099")
                    .estadoCancelacion("CANCELADO")
                    .estadoTicket("PENDIENTE_EVIDENCIA")
                    .fechaLimiteEvidencia(LocalDateTime.now().minusHours(2))
                    .amonestacionGenerada(false)
                    .createdAt(LocalDateTime.now().minusDays(3).minusHours(2))
                    .build();

            when(cancelacionRepository.findByAmonestacionGeneradaFalse())
                    .thenReturn(List.of(vencida))
                    .thenReturn(List.of());

            cancelacionesUseCase.procesarTicketsFuerzaMayorVencidos();
            // Segunda llamada para verificar que no duplica
            cancelacionesUseCase.procesarTicketsFuerzaMayorVencidos();

            assertThat(vencida.getEstadoTicket()).isEqualTo("VENCIDO");
            assertThat(vencida.getAmonestacionGenerada()).isTrue();
            assertThat(vencida.getEsInjustificada()).isTrue();

            ArgumentCaptor<AmonestacionProveedorEntity> captor = ArgumentCaptor.forClass(AmonestacionProveedorEntity.class);
            verify(amonestacionRepository, times(1)).save(captor.capture());
            AmonestacionProveedorEntity amon = captor.getValue();
            assertThat(amon.getNivel()).isEqualTo(NivelAmonestacion.GRAVE);
            assertThat(amon.getDiasRestriccion()).isEqualTo(7);
            assertThat(amon.getTicketCodigo()).isEqualTo("TKT-2026-0099");
        }
    }

    @Nested
    @DisplayName("3. Flujo de Cancelaciones Categoría 3 — Ventana móvil de 30 días (1, 3, 4, 6, 7+)")
    class Categoria3EscalonamientoTests {

        private List<CancelacionProveedorEntity> construirHistorialInjustificadasEnVentana(int cantidad) {
            List<CancelacionProveedorEntity> lista = new ArrayList<>();
            for (int i = 1; i <= cantidad; i++) {
                lista.add(CancelacionProveedorEntity.builder()
                        .id((long) i)
                        .solicitudServicioId(100L + i)
                        .perfilProveedorId(10L)
                        .categoria(CategoriaCancelacionProveedor.CATEGORIA_3_PERSONAL_OPERATIVO)
                        .motivo("Problemas de transporte")
                        .esInjustificada(true)
                        .createdAt(LocalDateTime.now().minusDays(i))
                        .build());
            }
            return lista;
        }

        private CancelacionProveedorResponse ejecutarCancelacionCategoria3ConConteoEnVentana(int conteoDespuesDeGuardar) {
            solicitudAceptada.setEstadoSolicitud("ACEPTADA");
            ofertaAceptada.setEstadoOferta("ACEPTADA");
            when(cancelacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(10L))
                    .thenReturn(construirHistorialInjustificadasEnVentana(conteoDespuesDeGuardar));

            return cancelacionesUseCase.solicitarCancelacion(new SolicitarCancelacionProveedorRequest(
                    100L,
                    10L,
                    CategoriaCancelacionProveedor.CATEGORIA_3_PERSONAL_OPERATIVO,
                    "Problemas de transporte",
                    "Vehículo averiado",
                    null,
                    null,
                    null
            ));
        }

        @Test
        @DisplayName("Primera cancelación injustificada (1 de 3) -> Amonestación LEVE (0 días de restricción)")
        void primeraCancelacion_generaAmonestacionLeve() {
            CancelacionProveedorResponse res = ejecutarCancelacionCategoria3ConConteoEnVentana(1);

            assertThat(res.amonestacionAsociada()).isNotNull();
            assertThat(res.amonestacionAsociada().nivel()).isEqualTo(NivelAmonestacion.LEVE);
            assertThat(res.amonestacionAsociada().diasRestriccion()).isEqualTo(0);
            assertThat(res.amonestacionAsociada().conteo30Dias()).isEqualTo(1);
        }

        @Test
        @DisplayName("Tercera cancelación injustificada (3 de 3) -> Amonestación LEVE (0 días de restricción)")
        void terceraCancelacion_generaAmonestacionLeve() {
            CancelacionProveedorResponse res = ejecutarCancelacionCategoria3ConConteoEnVentana(3);

            assertThat(res.amonestacionAsociada()).isNotNull();
            assertThat(res.amonestacionAsociada().nivel()).isEqualTo(NivelAmonestacion.LEVE);
            assertThat(res.amonestacionAsociada().diasRestriccion()).isEqualTo(0);
            assertThat(res.amonestacionAsociada().conteo30Dias()).isEqualTo(3);
        }

        @Test
        @DisplayName("Cuarta cancelación injustificada (4) -> Amonestación MODERADA (3 días de restricción)")
        void cuartaCancelacion_generaAmonestacionModerada3Dias() {
            CancelacionProveedorResponse res = ejecutarCancelacionCategoria3ConConteoEnVentana(4);

            assertThat(res.amonestacionAsociada()).isNotNull();
            assertThat(res.amonestacionAsociada().nivel()).isEqualTo(NivelAmonestacion.MODERADA);
            assertThat(res.amonestacionAsociada().diasRestriccion()).isEqualTo(3);
            assertThat(res.amonestacionAsociada().restriccionFin()).isAfter(LocalDateTime.now().plusDays(2));
        }

        @Test
        @DisplayName("Sexta cancelación injustificada (6) -> Amonestación MODERADA (3 días de restricción)")
        void sextaCancelacion_generaAmonestacionModerada3Dias() {
            CancelacionProveedorResponse res = ejecutarCancelacionCategoria3ConConteoEnVentana(6);

            assertThat(res.amonestacionAsociada()).isNotNull();
            assertThat(res.amonestacionAsociada().nivel()).isEqualTo(NivelAmonestacion.MODERADA);
            assertThat(res.amonestacionAsociada().diasRestriccion()).isEqualTo(3);
        }

        @Test
        @DisplayName("Séptima cancelación injustificada (7+) -> Amonestación GRAVE (7 días de restricción)")
        void septimaCancelacion_generaAmonestacionGrave7Dias() {
            CancelacionProveedorResponse res = ejecutarCancelacionCategoria3ConConteoEnVentana(7);

            assertThat(res.amonestacionAsociada()).isNotNull();
            assertThat(res.amonestacionAsociada().nivel()).isEqualTo(NivelAmonestacion.GRAVE);
            assertThat(res.amonestacionAsociada().diasRestriccion()).isEqualTo(7);
            assertThat(res.amonestacionAsociada().restriccionFin()).isAfter(LocalDateTime.now().plusDays(6));
        }

        @Test
        @DisplayName("Previsualización y ventana móvil de 30 días: ignora registros mayores a 30 días")
        void previsualizacionYVentanaMovil30Dias() {
            // 3 dentro de los últimos 30 días + 2 antiguas de hace 40 días
            List<CancelacionProveedorEntity> mixtas = new ArrayList<>(construirHistorialInjustificadasEnVentana(3));
            mixtas.add(CancelacionProveedorEntity.builder()
                    .id(99L)
                    .solicitudServicioId(199L)
                    .perfilProveedorId(10L)
                    .categoria(CategoriaCancelacionProveedor.CATEGORIA_3_PERSONAL_OPERATIVO)
                    .motivo("Antigua")
                    .esInjustificada(true)
                    .createdAt(LocalDateTime.now().minusDays(40))
                    .build());

            when(cancelacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(10L)).thenReturn(mixtas);
            when(amonestacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

            PrevisualizacionCancelacionResponse preview = cancelacionesUseCase.previsualizarCancelacion(
                    10L, CategoriaCancelacionProveedor.CATEGORIA_3_PERSONAL_OPERATIVO);

            // Solo cuenta las 3 de los últimos 30 días; si confirma será la #4 -> MODERADA (3 días)
            assertThat(preview.cancelacionesInjustificadasUltimos30Dias()).isEqualTo(3);
            assertThat(preview.conteoProyectadoSiConfirma()).isEqualTo(4);
            assertThat(preview.nivelProyectadoSiConfirma()).isEqualTo(NivelAmonestacion.MODERADA);
            assertThat(preview.diasRestriccionProyectados()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("4. Flujo de Cancelaciones Categoría 4 — Otros motivos")
    class Categoria4Tests {

        @Test
        @DisplayName("Categoría 4 justificada por soporte -> No genera amonestación")
        void categoria4_justificada_noGeneraAmonestacion() {
            CancelacionProveedorEntity entity = CancelacionProveedorEntity.builder()
                    .id(940L)
                    .solicitudServicioId(100L)
                    .ofertaId(500L)
                    .perfilProveedorId(10L)
                    .categoria(CategoriaCancelacionProveedor.CATEGORIA_4_OTROS)
                    .motivo("OTRO_MOTIVO")
                    .descripcion("Corte eléctrico general en la zona")
                    .ticketCodigo("TKT-2026-0040")
                    .estadoCancelacion("CANCELADO")
                    .estadoTicket("PENDIENTE_REVISION")
                    .createdAt(LocalDateTime.now())
                    .build();
            when(cancelacionRepository.findById(940L)).thenReturn(Optional.of(entity));

            CancelacionProveedorResponse res = cancelacionesUseCase.resolverPorSoporte(940L,
                    new ResolverCancelacionSoporteRequest(1L, true, "Corte de energía confirmado en el sector"));

            assertThat(res.decisionSoporte()).isEqualTo("APROBADO");
            assertThat(res.esInjustificada()).isFalse();
            assertThat(res.amonestacionAsociada()).isNull();
            verify(amonestacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Categoría 4 injustificada por soporte -> Suma a la ventana de 30 días y genera amonestación")
        void categoria4_injustificada_sumaAVentana30DiasYGeneraAmonestacion() {
            CancelacionProveedorEntity entity = CancelacionProveedorEntity.builder()
                    .id(941L)
                    .solicitudServicioId(100L)
                    .ofertaId(500L)
                    .perfilProveedorId(10L)
                    .categoria(CategoriaCancelacionProveedor.CATEGORIA_4_OTROS)
                    .motivo("OTRO_MOTIVO")
                    .descripcion("Olvidé que tenía otro compromiso")
                    .ticketCodigo("TKT-2026-0041")
                    .estadoCancelacion("CANCELADO")
                    .estadoTicket("PENDIENTE_REVISION")
                    .createdAt(LocalDateTime.now())
                    .build();
            when(cancelacionRepository.findById(941L)).thenReturn(Optional.of(entity));

            List<CancelacionProveedorEntity> cuatroInjustificadas = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                cuatroInjustificadas.add(CancelacionProveedorEntity.builder()
                        .id((long) i)
                        .solicitudServicioId(100L + i)
                        .perfilProveedorId(10L)
                        .categoria(CategoriaCancelacionProveedor.CATEGORIA_4_OTROS)
                        .motivo("OTRO")
                        .esInjustificada(true)
                        .createdAt(LocalDateTime.now().minusDays(i))
                        .build());
            }
            when(cancelacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(10L))
                    .thenReturn(cuatroInjustificadas);

            CancelacionProveedorResponse res = cancelacionesUseCase.resolverPorSoporte(941L,
                    new ResolverCancelacionSoporteRequest(1L, false, "Motivo personal no justificado"));

            assertThat(res.decisionSoporte()).isEqualTo("RECHAZADO");
            assertThat(res.esInjustificada()).isTrue();
            assertThat(res.amonestacionAsociada()).isNotNull();
            assertThat(res.amonestacionAsociada().nivel()).isEqualTo(NivelAmonestacion.MODERADA);
            assertThat(res.amonestacionAsociada().diasRestriccion()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("5. Restricción de Proveedor y Levantamiento Automático")
    class RestriccionProveedorTests {

        @Test
        @DisplayName("Proveedor con restricción activa queda bloqueado para ofertar; al vencer la restricción vuelve a operar")
        void proveedorRestringido_bloqueadoDuranteVigencia_yLiberadoAlVencer() {
            AmonestacionProveedorEntity restriccionActiva = AmonestacionProveedorEntity.builder()
                    .id(777L)
                    .perfilProveedorId(10L)
                    .solicitudServicioId(100L)
                    .cancelacionId(900L)
                    .nivel(NivelAmonestacion.MODERADA)
                    .motivo("4 cancelaciones en 30 días")
                    .conteo30Dias(4)
                    .diasRestriccion(3)
                    .restriccionInicio(LocalDateTime.now().minusDays(1))
                    .restriccionFin(LocalDateTime.now().plusDays(2))
                    .activa(true)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(amonestacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(10L))
                    .thenReturn(List.of(restriccionActiva));

            assertThat(cancelacionesUseCase.tieneRestriccionVigente(10L)).isTrue();
            assertThatThrownBy(() -> cancelacionesUseCase.validarProveedorSinRestriccionVigente(10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("restricción vigente");

            // Simulamos que la fecha fin ya venció y se ejecuta finalizarRestriccionesVencidas()
            restriccionActiva.setRestriccionFin(LocalDateTime.now().minusMinutes(5));
            when(amonestacionRepository.findByActivaTrue()).thenReturn(List.of(restriccionActiva));

            cancelacionesUseCase.finalizarRestriccionesVencidas();

            assertThat(restriccionActiva.getActiva()).isFalse();
            assertThat(cancelacionesUseCase.tieneRestriccionVigente(10L)).isFalse();
            HistorialDisciplinarioProveedorResponse historial = cancelacionesUseCase.obtenerHistorialDisciplinario(10L);
            assertThat(historial.tieneRestriccionVigente()).isFalse();
        }
    }

    @Nested
    @DisplayName("6. PIN de 4 dígitos (dev vs qa/pdn), Proximidad GPS <= 20m, Alarma 1h y Auditoría Interna")
    class PinGpsAlarmaAuditoriaTests {

        @Test
        @DisplayName("PIN: En perfil dev es 1234; en perfil pdn es aleatorio de 4 dígitos encriptado y solo visible al cliente")
        void pinSeguridad_devVsPdn_yVisibilidadExclusivaCliente() {
            assertThat(pinSeguridadHelperDev.generarPinPlano()).isEqualTo("1234");

            String pinPdn = pinSeguridadHelperPdn.generarPinPlano();
            assertThat(pinPdn).matches("^\\d{4}$");
            String encrypted = pinSeguridadHelperPdn.encriptarPin(pinPdn);
            assertThat(encrypted).startsWith("ENC:");
            assertThat(pinSeguridadHelperPdn.desencriptarPin(encrypted)).isEqualTo(pinPdn);

            // Visibilidad: cuando consulta el cliente dueño (obtenerSolicitudParaCliente), ve pinInicio; en vista general es null
            SolicitudResponse vistaCliente = solicitudesUseCase.obtenerSolicitudParaCliente(100L, 200L);
            assertThat(vistaCliente.pinInicio()).isEqualTo("1234");

            SolicitudResponse vistaProveedor = solicitudesUseCase.obtenerSolicitudPorId(100L);
            assertThat(vistaProveedor.pinInicio()).isNull();
        }

        @Test
        @DisplayName("Iniciar Servicio (ACEPTADA -> EN_CURSO): Rechaza PIN incorrecto o distancia GPS > 20m")
        void iniciarServicio_rechazaPinIncorrectoODistanciaMayorA20m() {
            // 1. PIN incorrecto
            IniciarServicioRequest reqPinMalo = new IniciarServicioRequest(
                    "9999",
                    new BigDecimal("6.2442030"),
                    new BigDecimal("-75.5812119"),
                    10L
            );

            assertThatThrownBy(() -> solicitudesUseCase.iniciarServicio(100L, reqPinMalo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PIN de 4 dígitos ingresado no es válido");

            // 2. PIN correcto pero distancia > 20 metros (~110 metros de diferencia)
            IniciarServicioRequest reqLejos = new IniciarServicioRequest(
                    "1234",
                    new BigDecimal("6.2452030"),
                    new BigDecimal("-75.5812119"),
                    10L
            );

            assertThatThrownBy(() -> solicitudesUseCase.iniciarServicio(100L, reqLejos))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("20 metros");
        }

        @Test
        @DisplayName("Iniciar Servicio (ACEPTADA -> EN_CURSO): Con PIN válido y GPS <= 20m cambia a EN_CURSO y guarda auditoría GPS")
        void iniciarServicio_exitosoDentroDe20Metros_registraAuditoriaInterna() {
            // A ~4 metros de 6.2442030, -75.5812119
            IniciarServicioRequest reqValido = new IniciarServicioRequest(
                    "1234",
                    new BigDecimal("6.2442400"),
                    new BigDecimal("-75.5812119"),
                    10L
            );

            SolicitudResponse res = solicitudesUseCase.iniciarServicio(100L, reqValido);

            assertThat(res.estadoSolicitud()).isEqualTo("EN_CURSO");
            verify(auditoriaRepository, times(1)).save(argThat(aud ->
                    "ACEPTADA".equals(aud.getEstadoAnterior())
                            && "EN_CURSO".equals(aud.getEstadoNuevo())
                            && aud.getLatitud() != null
                            && aud.getDistanciaMetros() != null
                            && aud.getDistanciaMetros().doubleValue() <= 20.0
            ));
        }

        @Test
        @DisplayName("Alarma a 1 hora del servicio: Envía notificación al proveedor asignado y marca recordatorio1hEnviado=true")
        void alarma1HoraAntes_notificaProveedorIdempotentemente() {
            when(solicitudService.findByEstado("ACEPTADA")).thenReturn(List.of(solicitudAceptada));
            when(solicitudService.findByEstado("PROGRAMADA")).thenReturn(List.of());

            solicitudesUseCase.enviarAlarmasUnaHoraAntes();
            // Segunda ejecución no debe reenviar porque recordatorio1hEnviado ya quedó en true
            solicitudesUseCase.enviarAlarmasUnaHoraAntes();

            assertThat(solicitudAceptada.getRecordatorio1hEnviado()).isTrue();
            verify(eventPublisher, times(1)).notifyAlarmaServicio1Hora(eq(solicitudAceptada), eq(ofertaAceptada), eq(proveedor));
        }
    }
}
