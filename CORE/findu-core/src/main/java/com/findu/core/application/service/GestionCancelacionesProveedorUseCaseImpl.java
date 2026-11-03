package com.findu.core.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.core.application.dto.request.AdjuntarEvidenciaCancelacionRequest;
import com.findu.core.application.dto.request.ResolverCancelacionSoporteRequest;
import com.findu.core.application.dto.request.SolicitarCancelacionProveedorRequest;
import com.findu.core.application.dto.response.*;
import com.findu.core.application.port.in.GestionCancelacionesProveedorUseCase;
import com.findu.core.application.port.out.externalapi.NotificationPort;
import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import com.findu.core.domain.enums.NivelAmonestacion;
import com.findu.core.domain.enums.NotificationTemplates;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.infrastructure.adapter.out.externalapi.HelpServiceClient;
import com.findu.core.infrastructure.adapter.out.persistence.entity.AmonestacionProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.CancelacionProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.AmonestacionProveedorRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.CancelacionProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GestionCancelacionesProveedorUseCaseImpl implements GestionCancelacionesProveedorUseCase {

    public static final int VENTANA_MOVIL_DIAS = 30;
    public static final int PLAZO_EVIDENCIA_FUERZA_MAYOR_DIAS = 3;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final CancelacionProveedorRepository cancelacionRepository;
    private final AmonestacionProveedorRepository amonestacionRepository;
    private final SolicitudServicioService solicitudService;
    private final OfertaService ofertaService;
    private final PerfilProveedorService proveedorService;
    private final PerfilClienteService perfilClienteService;
    private final SolicitudEstadoAuditoriaService auditoriaService;
    private final HelpServiceClient helpServiceClient;
    private final NotificationPort notificationPort;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaCancelacionCatalogoResponse> obtenerCatalogoCategorias() {
        return Arrays.stream(CategoriaCancelacionProveedor.values())
                .map(c -> new CategoriaCancelacionCatalogoResponse(
                        c,
                        c.getTitulo(),
                        c.getAdvertenciaBase(),
                        c == CategoriaCancelacionProveedor.CATEGORIA_4_OTROS,
                        c == CategoriaCancelacionProveedor.CATEGORIA_1_CLIENTE || c == CategoriaCancelacionProveedor.CATEGORIA_4_OTROS,
                        c == CategoriaCancelacionProveedor.CATEGORIA_2_FUERZA_MAYOR,
                        c.getMotivosPermitidos()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrevisualizacionCancelacionResponse previsualizarCancelacion(Long proveedorId, CategoriaCancelacionProveedor categoria) {
        if (proveedorId == null) {
            throw new IllegalArgumentException("El ID del proveedor es obligatorio");
        }
        CategoriaCancelacionProveedor cat = categoria != null ? categoria : CategoriaCancelacionProveedor.CATEGORIA_3_PERSONAL_OPERATIVO;

        int cancelacionesInjustificadas30d = contarCancelacionesInjustificadasUltimos30Dias(proveedorId);
        int amonestaciones30d = contarAmonestacionesUltimos30Dias(proveedorId);

        if (cat == CategoriaCancelacionProveedor.CATEGORIA_1_CLIENTE) {
            return new PrevisualizacionCancelacionResponse(
                    proveedorId,
                    cat,
                    cat.getAdvertenciaBase(),
                    cancelacionesInjustificadas30d,
                    amonestaciones30d,
                    cancelacionesInjustificadas30d,
                    null,
                    0,
                    "No genera amonestaciones al proveedor cuando soporte valida el motivo.",
                    false
            );
        }

        if (cat == CategoriaCancelacionProveedor.CATEGORIA_2_FUERZA_MAYOR) {
            return new PrevisualizacionCancelacionResponse(
                    proveedorId,
                    cat,
                    cat.getAdvertenciaBase(),
                    cancelacionesInjustificadas30d,
                    amonestaciones30d,
                    cancelacionesInjustificadas30d,
                    NivelAmonestacion.GRAVE,
                    7,
                    "Sin amonestación si presentas soporte válido dentro de 3 días; amonestación GRAVE (7 días de restricción) si no presentas soporte válido.",
                    true
            );
        }

        int conteoProyectado = cancelacionesInjustificadas30d + 1;
        NivelAmonestacion nivelProyectado = NivelAmonestacion.porConteoInjustificadas(conteoProyectado);
        int diasRestriccion = nivelProyectado.getDiasRestriccion();

        String advertenciaDinamica = String.format(
                "En los últimos 30 días registras %d cancelación(es) injustificada(s) y %d amonestación(es). " +
                "Si confirmas esta cancelación (y se determina injustificada), alcanzarás %d cancelación(es) en 30 días, " +
                "lo que corresponde a una amonestación de nivel %s (%s).",
                cancelacionesInjustificadas30d,
                amonestaciones30d,
                conteoProyectado,
                nivelProyectado.name(),
                nivelProyectado.getConsecuencia()
        );

        return new PrevisualizacionCancelacionResponse(
                proveedorId,
                cat,
                advertenciaDinamica,
                cancelacionesInjustificadas30d,
                amonestaciones30d,
                conteoProyectado,
                nivelProyectado,
                diasRestriccion,
                nivelProyectado.getConsecuencia(),
                true
        );
    }

    @Override
    public CancelacionProveedorResponse solicitarCancelacion(SolicitarCancelacionProveedorRequest request) {
        if (request == null || request.solicitudServicioId() == null || request.perfilProveedorId() == null || request.categoria() == null) {
            throw new IllegalArgumentException("Solicitud, proveedor y categoría son obligatorios para cancelar");
        }
        if (request.motivo() == null || request.motivo().isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar o indicar el motivo de cancelación");
        }
        if (request.categoria() == CategoriaCancelacionProveedor.CATEGORIA_4_OTROS
                && (request.descripcion() == null || request.descripcion().isBlank())) {
            throw new IllegalArgumentException("La descripción es obligatoria cuando selecciona la categoría 'Otros motivos'");
        }

        SolicitudServicio solicitud = solicitudService.findById(request.solicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + request.solicitudServicioId()));

        PerfilProveedor proveedor = proveedorService.findById(request.perfilProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + request.perfilProveedorId()));

        PerfilCliente cliente = solicitud.getPerfilClienteId() != null
                ? perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null)
                : null;

        Oferta ofertaProveedor = ofertaService.findBySolicitudId(solicitud.getId()).stream()
                .filter(o -> request.perfilProveedorId().equals(o.getPerfilProveedorId()))
                .findFirst()
                .orElse(null);

        String estadoSolicitudAnterior = solicitud.getEstadoSolicitud();
        LocalDateTime ahora = LocalDateTime.now();

        return switch (request.categoria()) {
            case CATEGORIA_1_CLIENTE -> procesarCategoria1Cliente(
                    request, solicitud, proveedor, cliente, ofertaProveedor, estadoSolicitudAnterior, ahora);
            case CATEGORIA_2_FUERZA_MAYOR -> procesarCategoria2FuerzaMayor(
                    request, solicitud, proveedor, cliente, ofertaProveedor, estadoSolicitudAnterior, ahora);
            case CATEGORIA_3_PERSONAL_OPERATIVO -> procesarCategoria3PersonalOperativo(
                    request, solicitud, proveedor, cliente, ofertaProveedor, estadoSolicitudAnterior, ahora);
            case CATEGORIA_4_OTROS -> procesarCategoria4Otros(
                    request, solicitud, proveedor, cliente, ofertaProveedor, estadoSolicitudAnterior, ahora);
        };
    }

    private CancelacionProveedorResponse procesarCategoria1Cliente(
            SolicitarCancelacionProveedorRequest request,
            SolicitudServicio solicitud,
            PerfilProveedor proveedor,
            PerfilCliente cliente,
            Oferta ofertaProveedor,
            String estadoSolicitudAnterior,
            LocalDateTime ahora
    ) {
        String ticketCodigo = helpServiceClient != null
                ? helpServiceClient.crearTicketCancelacion(
                        proveedor.getId(), proveedor.getNombreCompleto(), solicitud.getId(),
                        request.categoria(), request.motivo(), request.descripcion())
                : "TKT-C1-" + solicitud.getId();

        // El servicio permanece pendiente de resolución y NO se libera automáticamente
        solicitud.setEstadoSolicitud("PENDIENTE_REVISION_SOPORTE");
        solicitudService.save(solicitud);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    solicitud.getId(),
                    estadoSolicitudAnterior,
                    "PENDIENTE_REVISION_SOPORTE",
                    "PROVEEDOR",
                    proveedor.getId(),
                    request.latitud(),
                    request.longitud(),
                    null,
                    "Solicitud de cancelación Categoría 1 (Cliente): " + request.motivo()
            );
        }

        CancelacionProveedorEntity entity = CancelacionProveedorEntity.builder()
                .solicitudServicioId(solicitud.getId())
                .ofertaId(ofertaProveedor != null ? ofertaProveedor.getId() : null)
                .perfilProveedorId(proveedor.getId())
                .categoria(CategoriaCancelacionProveedor.CATEGORIA_1_CLIENTE)
                .motivo(request.motivo())
                .descripcion(request.descripcion())
                .evidenciasUrls(serializeList(request.evidenciasUrls()))
                .ticketCodigo(ticketCodigo)
                .estadoCancelacion("PENDIENTE_REVISION_SOPORTE")
                .estadoTicket("PENDIENTE_REVISION")
                .esInjustificada(false)
                .amonestacionGenerada(false)
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();

        CancelacionProveedorEntity guardada = cancelacionRepository.save(entity);

        enviarNotificacionProveedor(proveedor, NotificationTemplates.SOLICITUD_CANCELACION_RECIBIDA, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "cancelacionId", String.valueOf(guardada.getId()),
                "ticketCodigo", ticketCodigo,
                "categoria", request.categoria().name(),
                "estadoCancelacion", "PENDIENTE_REVISION_SOPORTE",
                "tipoEvento", "SOLICITUD_CANCELACION_RECIBIDA"
        ));
        enviarNotificacionProveedor(proveedor, NotificationTemplates.TICKET_CANCELACION_CREADO, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "ticketCodigo", ticketCodigo,
                "estadoTicket", "PENDIENTE_REVISION",
                "tipoEvento", "TICKET_CREADO"
        ));

        return toResponse(guardada, solicitud.getEstadoSolicitud(), null,
                CategoriaCancelacionProveedor.CATEGORIA_1_CLIENTE.getAdvertenciaBase());
    }

    private CancelacionProveedorResponse procesarCategoria2FuerzaMayor(
            SolicitarCancelacionProveedorRequest request,
            SolicitudServicio solicitud,
            PerfilProveedor proveedor,
            PerfilCliente cliente,
            Oferta ofertaProveedor,
            String estadoSolicitudAnterior,
            LocalDateTime ahora
    ) {
        String ticketCodigo = helpServiceClient != null
                ? helpServiceClient.crearTicketCancelacion(
                        proveedor.getId(), proveedor.getNombreCompleto(), solicitud.getId(),
                        request.categoria(), request.motivo(), request.descripcion())
                : "TKT-C2-" + solicitud.getId();

        // Se cancela la oferta del proveedor y el servicio queda liberado para que el cliente pueda reabrirlo
        if (ofertaProveedor != null) {
            ofertaProveedor.setEstadoOferta("CANCELADA_PROVEEDOR");
            ofertaService.save(ofertaProveedor);
        }
        solicitud.setEstadoSolicitud("CANCELADO");
        solicitudService.save(solicitud);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    solicitud.getId(),
                    estadoSolicitudAnterior,
                    "CANCELADO",
                    "PROVEEDOR",
                    proveedor.getId(),
                    request.latitud(),
                    request.longitud(),
                    null,
                    "Cancelación Categoría 2 (Fuerza mayor): " + request.motivo()
            );
        }

        boolean tieneEvidenciaInicial = request.evidenciasUrls() != null && !request.evidenciasUrls().isEmpty();
        String estadoTicketInicial = tieneEvidenciaInicial ? "PENDIENTE_REVISION" : "PENDIENTE_EVIDENCIA";
        LocalDateTime fechaLimite = ahora.plusDays(PLAZO_EVIDENCIA_FUERZA_MAYOR_DIAS);

        CancelacionProveedorEntity entity = CancelacionProveedorEntity.builder()
                .solicitudServicioId(solicitud.getId())
                .ofertaId(ofertaProveedor != null ? ofertaProveedor.getId() : null)
                .perfilProveedorId(proveedor.getId())
                .categoria(CategoriaCancelacionProveedor.CATEGORIA_2_FUERZA_MAYOR)
                .motivo(request.motivo())
                .descripcion(request.descripcion())
                .evidenciasUrls(serializeList(request.evidenciasUrls()))
                .ticketCodigo(ticketCodigo)
                .estadoCancelacion("CANCELADO")
                .estadoTicket(estadoTicketInicial)
                .esInjustificada(null)
                .fechaLimiteEvidencia(fechaLimite)
                .amonestacionGenerada(false)
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();

        CancelacionProveedorEntity guardada = cancelacionRepository.save(entity);

        // Notificaciones al proveedor
        enviarNotificacionProveedor(proveedor, NotificationTemplates.SOLICITUD_CANCELACION_RECIBIDA, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "cancelacionId", String.valueOf(guardada.getId()),
                "ticketCodigo", ticketCodigo,
                "fechaLimiteEvidencia", fechaLimite.toString(),
                "tipoEvento", "SOLICITUD_CANCELACION_RECIBIDA"
        ));
        enviarNotificacionProveedor(proveedor, NotificationTemplates.TICKET_CANCELACION_CREADO, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "ticketCodigo", ticketCodigo,
                "estadoTicket", estadoTicketInicial,
                "fechaLimiteEvidencia", fechaLimite.toString(),
                "tipoEvento", "TICKET_CREADO"
        ));
        if (tieneEvidenciaInicial) {
            enviarNotificacionProveedor(proveedor, NotificationTemplates.EVIDENCIA_CANCELACION_RECIBIDA, Map.of(
                    "solicitudId", String.valueOf(solicitud.getId()),
                    "ticketCodigo", ticketCodigo,
                    "tipoEvento", "EVIDENCIA_RECIBIDA"
            ));
        } else {
            enviarNotificacionProveedor(proveedor, NotificationTemplates.SOPORTE_PENDIENTE_CARGAR, Map.of(
                    "solicitudId", String.valueOf(solicitud.getId()),
                    "ticketCodigo", ticketCodigo,
                    "fechaLimiteEvidencia", fechaLimite.toString(),
                    "tipoEvento", "SOPORTE_PENDIENTE_CARGAR"
            ));
        }

        // Notificaciones al cliente (servicio cancelado por el proveedor + disponible para reabrir)
        enviarNotificacionCliente(cliente, solicitud.getPerfilClienteId(), NotificationTemplates.SERVICIO_CANCELADO_POR_PROVEEDOR, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "motivo", request.motivo(),
                "puedeReabrir", "true",
                "tipoEvento", "SERVICIO_CANCELADO_POR_PROVEEDOR"
        ));
        enviarNotificacionCliente(cliente, solicitud.getPerfilClienteId(), NotificationTemplates.SERVICIO_DISPONIBLE_PARA_REABRIR, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "puedeReabrir", "true",
                "tipoEvento", "SERVICIO_DISPONIBLE_PARA_REABRIR"
        ));

        return toResponse(guardada, solicitud.getEstadoSolicitud(), null,
                CategoriaCancelacionProveedor.CATEGORIA_2_FUERZA_MAYOR.getAdvertenciaBase());
    }

    private CancelacionProveedorResponse procesarCategoria3PersonalOperativo(
            SolicitarCancelacionProveedorRequest request,
            SolicitudServicio solicitud,
            PerfilProveedor proveedor,
            PerfilCliente cliente,
            Oferta ofertaProveedor,
            String estadoSolicitudAnterior,
            LocalDateTime ahora
    ) {
        if (ofertaProveedor != null) {
            ofertaProveedor.setEstadoOferta("CANCELADA_PROVEEDOR");
            ofertaService.save(ofertaProveedor);
        }
        solicitud.setEstadoSolicitud("CANCELADO");
        solicitudService.save(solicitud);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    solicitud.getId(),
                    estadoSolicitudAnterior,
                    "CANCELADO",
                    "PROVEEDOR",
                    proveedor.getId(),
                    request.latitud(),
                    request.longitud(),
                    null,
                    "Cancelación Categoría 3 (Personal u operativo): " + request.motivo()
            );
        }

        CancelacionProveedorEntity entity = CancelacionProveedorEntity.builder()
                .solicitudServicioId(solicitud.getId())
                .ofertaId(ofertaProveedor != null ? ofertaProveedor.getId() : null)
                .perfilProveedorId(proveedor.getId())
                .categoria(CategoriaCancelacionProveedor.CATEGORIA_3_PERSONAL_OPERATIVO)
                .motivo(request.motivo())
                .descripcion(request.descripcion())
                .evidenciasUrls(serializeList(request.evidenciasUrls()))
                .ticketCodigo(null)
                .estadoCancelacion("CANCELADO")
                .estadoTicket("APROBADO")
                .esInjustificada(true)
                .fechaResolucion(ahora)
                .amonestacionGenerada(false)
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();

        CancelacionProveedorEntity guardada = cancelacionRepository.save(entity);

        // Evaluar escala móvil de 30 días y generar amonestación de manera idempotente
        AmonestacionProveedorEntity amonestacion = generarAmonestacionSegunEscala30Dias(guardada, proveedor, ahora);

        enviarNotificacionCliente(cliente, solicitud.getPerfilClienteId(), NotificationTemplates.SERVICIO_CANCELADO_POR_PROVEEDOR, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "motivo", request.motivo(),
                "puedeReabrir", "true",
                "tipoEvento", "SERVICIO_CANCELADO_POR_PROVEEDOR"
        ));
        enviarNotificacionCliente(cliente, solicitud.getPerfilClienteId(), NotificationTemplates.SERVICIO_DISPONIBLE_PARA_REABRIR, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "puedeReabrir", "true",
                "tipoEvento", "SERVICIO_DISPONIBLE_PARA_REABRIR"
        ));

        return toResponse(guardada, solicitud.getEstadoSolicitud(), amonestacion,
                "Cancelación procesada. Se ha registrado una amonestación de nivel " + amonestacion.getNivel().name() + ".");
    }

    private CancelacionProveedorResponse procesarCategoria4Otros(
            SolicitarCancelacionProveedorRequest request,
            SolicitudServicio solicitud,
            PerfilProveedor proveedor,
            PerfilCliente cliente,
            Oferta ofertaProveedor,
            String estadoSolicitudAnterior,
            LocalDateTime ahora
    ) {
        String ticketCodigo = helpServiceClient != null
                ? helpServiceClient.crearTicketCancelacion(
                        proveedor.getId(), proveedor.getNombreCompleto(), solicitud.getId(),
                        request.categoria(), request.motivo(), request.descripcion())
                : "TKT-C4-" + solicitud.getId();

        if (ofertaProveedor != null) {
            ofertaProveedor.setEstadoOferta("CANCELADA_PROVEEDOR");
            ofertaService.save(ofertaProveedor);
        }
        solicitud.setEstadoSolicitud("CANCELADO");
        solicitudService.save(solicitud);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    solicitud.getId(),
                    estadoSolicitudAnterior,
                    "CANCELADO",
                    "PROVEEDOR",
                    proveedor.getId(),
                    request.latitud(),
                    request.longitud(),
                    null,
                    "Cancelación Categoría 4 (Otros motivos - en evaluación): " + request.descripcion()
            );
        }

        // No se asume automáticamente injustificada; queda pendiente de revisión de soporte
        CancelacionProveedorEntity entity = CancelacionProveedorEntity.builder()
                .solicitudServicioId(solicitud.getId())
                .ofertaId(ofertaProveedor != null ? ofertaProveedor.getId() : null)
                .perfilProveedorId(proveedor.getId())
                .categoria(CategoriaCancelacionProveedor.CATEGORIA_4_OTROS)
                .motivo(request.motivo())
                .descripcion(request.descripcion())
                .evidenciasUrls(serializeList(request.evidenciasUrls()))
                .ticketCodigo(ticketCodigo)
                .estadoCancelacion("CANCELADO")
                .estadoTicket("PENDIENTE_REVISION")
                .esInjustificada(null)
                .amonestacionGenerada(false)
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();

        CancelacionProveedorEntity guardada = cancelacionRepository.save(entity);

        enviarNotificacionProveedor(proveedor, NotificationTemplates.SOLICITUD_CANCELACION_RECIBIDA, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "cancelacionId", String.valueOf(guardada.getId()),
                "ticketCodigo", ticketCodigo,
                "estadoTicket", "PENDIENTE_REVISION",
                "tipoEvento", "SOLICITUD_CANCELACION_RECIBIDA"
        ));
        enviarNotificacionProveedor(proveedor, NotificationTemplates.TICKET_CANCELACION_CREADO, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "ticketCodigo", ticketCodigo,
                "estadoTicket", "PENDIENTE_REVISION",
                "tipoEvento", "TICKET_CREADO"
        ));

        enviarNotificacionCliente(cliente, solicitud.getPerfilClienteId(), NotificationTemplates.SERVICIO_CANCELADO_POR_PROVEEDOR, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "motivo", request.motivo(),
                "puedeReabrir", "true",
                "tipoEvento", "SERVICIO_CANCELADO_POR_PROVEEDOR"
        ));
        enviarNotificacionCliente(cliente, solicitud.getPerfilClienteId(), NotificationTemplates.SERVICIO_DISPONIBLE_PARA_REABRIR, Map.of(
                "solicitudId", String.valueOf(solicitud.getId()),
                "puedeReabrir", "true",
                "tipoEvento", "SERVICIO_DISPONIBLE_PARA_REABRIR"
        ));

        return toResponse(guardada, solicitud.getEstadoSolicitud(), null,
                "Cancelación registrada y enviada a soporte para evaluar si el motivo está justificado.");
    }

    @Override
    public CancelacionProveedorResponse adjuntarEvidencias(Long cancelacionId, AdjuntarEvidenciaCancelacionRequest request) {
        CancelacionProveedorEntity cancelacion = cancelacionRepository.findById(cancelacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de cancelación no encontrada con id: " + cancelacionId));

        if (request.perfilProveedorId() != null && !request.perfilProveedorId().equals(cancelacion.getPerfilProveedorId())) {
            throw new IllegalStateException("La solicitud de cancelación no pertenece al proveedor indicado");
        }
        if ("VENCIDO".equalsIgnoreCase(cancelacion.getEstadoTicket())) {
            throw new IllegalStateException("El plazo de 3 días para presentar soportes ya venció");
        }

        List<String> actuales = new ArrayList<>(deserializeList(cancelacion.getEvidenciasUrls()));
        if (request.evidenciasUrls() != null) {
            actuales.addAll(request.evidenciasUrls());
        }
        cancelacion.setEvidenciasUrls(serializeList(actuales));
        cancelacion.setEstadoTicket("PENDIENTE_REVISION");
        cancelacion.setUpdatedAt(LocalDateTime.now());

        CancelacionProveedorEntity guardada = cancelacionRepository.save(cancelacion);

        PerfilProveedor proveedor = proveedorService.findById(cancelacion.getPerfilProveedorId()).orElse(null);
        enviarNotificacionProveedor(proveedor, NotificationTemplates.EVIDENCIA_CANCELACION_RECIBIDA, Map.of(
                "solicitudId", String.valueOf(cancelacion.getSolicitudServicioId()),
                "cancelacionId", String.valueOf(cancelacion.getId()),
                "ticketCodigo", cancelacion.getTicketCodigo() != null ? cancelacion.getTicketCodigo() : "",
                "estadoTicket", "PENDIENTE_REVISION",
                "tipoEvento", "EVIDENCIA_RECIBIDA"
        ));

        SolicitudServicio solicitud = solicitudService.findById(cancelacion.getSolicitudServicioId()).orElse(null);
        AmonestacionProveedorEntity amon = amonestacionRepository.findByCancelacionId(cancelacion.getId()).orElse(null);

        return toResponse(guardada, solicitud != null ? solicitud.getEstadoSolicitud() : null, amon,
                "Evidencias recibidas correctamente y enviadas a validación de soporte.");
    }

    @Override
    public CancelacionProveedorResponse resolverPorSoporte(Long cancelacionId, ResolverCancelacionSoporteRequest request) {
        CancelacionProveedorEntity cancelacion = cancelacionRepository.findById(cancelacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de cancelación no encontrada con id: " + cancelacionId));

        SolicitudServicio solicitud = solicitudService.findById(cancelacion.getSolicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de servicio no encontrada"));

        PerfilProveedor proveedor = proveedorService.findById(cancelacion.getPerfilProveedorId()).orElse(null);
        PerfilCliente cliente = solicitud.getPerfilClienteId() != null
                ? perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null)
                : null;

        boolean aprobar = Boolean.TRUE.equals(request.aprobar());
        LocalDateTime ahora = LocalDateTime.now();
        String estadoAnterior = solicitud.getEstadoSolicitud();

        cancelacion.setAgenteSoporteId(request.agenteSoporteId() != null ? request.agenteSoporteId() : 1L);
        cancelacion.setJustificacionSoporte(request.justificacion());
        cancelacion.setFechaResolucion(ahora);
        cancelacion.setUpdatedAt(ahora);

        AmonestacionProveedorEntity amonestacionGenerada = null;

        switch (cancelacion.getCategoria()) {
            case CATEGORIA_1_CLIENTE -> {
                if (aprobar) {
                    // Únicamente soporte puede ejecutar la cancelación definitiva en Categoría 1
                    cancelacion.setDecisionSoporte("APROBADO");
                    cancelacion.setEstadoTicket("APROBADO");
                    cancelacion.setEstadoCancelacion("CANCELADO");
                    cancelacion.setEsInjustificada(false);

                    solicitud.setEstadoSolicitud("CANCELADO");
                    solicitudService.save(solicitud);

                    if (cancelacion.getOfertaId() != null) {
                        ofertaService.findById(cancelacion.getOfertaId()).ifPresent(o -> {
                            o.setEstadoOferta("CANCELADA");
                            ofertaService.save(o);
                        });
                    }

                    if (auditoriaService != null) {
                        auditoriaService.registrarCambioEstado(
                                solicitud.getId(), estadoAnterior, "CANCELADO",
                                "SOPORTE", cancelacion.getAgenteSoporteId(), null, null, null,
                                "Cancelación Categoría 1 aprobada por soporte: " + request.justificacion()
                        );
                    }

                    enviarNotificacionCliente(cliente, solicitud.getPerfilClienteId(), NotificationTemplates.CANCELACION_EJECUTADA_POR_SOPORTE, Map.of(
                            "solicitudId", String.valueOf(solicitud.getId()),
                            "motivo", cancelacion.getMotivo(),
                            "justificacionSoporte", request.justificacion() != null ? request.justificacion() : "",
                            "tipoEvento", "CANCELACION_EJECUTADA_POR_SOPORTE"
                    ));
                    enviarNotificacionProveedor(proveedor, NotificationTemplates.RESOLUCION_SOLICITUD_CANCELACION, Map.of(
                            "solicitudId", String.valueOf(solicitud.getId()),
                            "cancelacionId", String.valueOf(cancelacion.getId()),
                            "decision", "APROBADO",
                            "estadoServicio", "CANCELADO",
                            "justificacion", request.justificacion() != null ? request.justificacion() : "",
                            "tipoEvento", "RESOLUCION_CANCELACION"
                    ));
                } else {
                    // Si soporte rechaza la solicitud en Categoría 1, el servicio continúa disponible (ACEPTADA) para el proveedor
                    cancelacion.setDecisionSoporte("RECHAZADO");
                    cancelacion.setEstadoTicket("RECHAZADO");
                    cancelacion.setEstadoCancelacion("CANCELACION_RECHAZADA");
                    cancelacion.setEsInjustificada(false);

                    solicitud.setEstadoSolicitud("ACEPTADA");
                    solicitudService.save(solicitud);

                    if (auditoriaService != null) {
                        auditoriaService.registrarCambioEstado(
                                solicitud.getId(), estadoAnterior, "ACEPTADA",
                                "SOPORTE", cancelacion.getAgenteSoporteId(), null, null, null,
                                "Cancelación Categoría 1 rechazada por soporte (servicio continúa activo): " + request.justificacion()
                        );
                    }

                    enviarNotificacionProveedor(proveedor, NotificationTemplates.RESOLUCION_SOLICITUD_CANCELACION, Map.of(
                            "solicitudId", String.valueOf(solicitud.getId()),
                            "cancelacionId", String.valueOf(cancelacion.getId()),
                            "decision", "RECHAZADO",
                            "estadoServicio", "ACEPTADA",
                            "justificacion", request.justificacion() != null ? request.justificacion() : "Solicitud de cancelación rechazada por soporte",
                            "tipoEvento", "RESOLUCION_CANCELACION"
                    ));
                }
            }
            case CATEGORIA_2_FUERZA_MAYOR -> {
                if (aprobar) {
                    // Soporte válido: se cierra el proceso sin generar amonestación
                    cancelacion.setDecisionSoporte("APROBADO");
                    cancelacion.setEstadoTicket("APROBADO");
                    cancelacion.setEsInjustificada(false);

                    enviarNotificacionProveedor(proveedor, NotificationTemplates.EVIDENCIA_CANCELACION_RESUELTA, Map.of(
                            "solicitudId", String.valueOf(solicitud.getId()),
                            "cancelacionId", String.valueOf(cancelacion.getId()),
                            "ticketCodigo", cancelacion.getTicketCodigo() != null ? cancelacion.getTicketCodigo() : "",
                            "decision", "APROBADO",
                            "justificacion", request.justificacion() != null ? request.justificacion() : "Soporte de fuerza mayor validado",
                            "tipoEvento", "EVIDENCIA_APROBADA"
                    ));
                } else {
                    // Soporte inválido: queda registrada la decisión y se mantiene seguimiento hasta el vencimiento de los 3 días
                    cancelacion.setDecisionSoporte("RECHAZADO");
                    cancelacion.setEstadoTicket("RECHAZADO");

                    enviarNotificacionProveedor(proveedor, NotificationTemplates.EVIDENCIA_CANCELACION_RESUELTA, Map.of(
                            "solicitudId", String.valueOf(solicitud.getId()),
                            "cancelacionId", String.valueOf(cancelacion.getId()),
                            "ticketCodigo", cancelacion.getTicketCodigo() != null ? cancelacion.getTicketCodigo() : "",
                            "decision", "RECHAZADO",
                            "justificacion", request.justificacion() != null ? request.justificacion() : "El soporte adjuntado no es válido",
                            "fechaLimiteEvidencia", cancelacion.getFechaLimiteEvidencia() != null ? cancelacion.getFechaLimiteEvidencia().toString() : "",
                            "tipoEvento", "EVIDENCIA_RECHAZADA"
                    ));
                }
            }
            case CATEGORIA_4_OTROS -> {
                if (aprobar) {
                    // Soporte determina que está justificada: no genera amonestación
                    cancelacion.setDecisionSoporte("APROBADO");
                    cancelacion.setEstadoTicket("APROBADO");
                    cancelacion.setEsInjustificada(false);

                    enviarNotificacionProveedor(proveedor, NotificationTemplates.RESOLUCION_SOLICITUD_CANCELACION, Map.of(
                            "solicitudId", String.valueOf(solicitud.getId()),
                            "cancelacionId", String.valueOf(cancelacion.getId()),
                            "decision", "JUSTIFICADA",
                            "justificacion", request.justificacion() != null ? request.justificacion() : "Motivo validado por soporte",
                            "tipoEvento", "RESOLUCION_CANCELACION"
                    ));
                } else {
                    // Soporte determina que es injustificada: se contabiliza en la ventana móvil de 30 días y genera amonestación
                    cancelacion.setDecisionSoporte("RECHAZADO");
                    cancelacion.setEstadoTicket("RECHAZADO");
                    cancelacion.setEsInjustificada(true);

                    cancelacion = cancelacionRepository.save(cancelacion);
                    amonestacionGenerada = generarAmonestacionSegunEscala30Dias(cancelacion, proveedor, ahora);

                    enviarNotificacionProveedor(proveedor, NotificationTemplates.RESOLUCION_SOLICITUD_CANCELACION, Map.of(
                            "solicitudId", String.valueOf(solicitud.getId()),
                            "cancelacionId", String.valueOf(cancelacion.getId()),
                            "decision", "INJUSTIFICADA",
                            "nivelAmonestacion", amonestacionGenerada.getNivel().name(),
                            "justificacion", request.justificacion() != null ? request.justificacion() : "Cancelación determinada como injustificada",
                            "tipoEvento", "RESOLUCION_CANCELACION"
                    ));
                }
            }
            case CATEGORIA_3_PERSONAL_OPERATIVO -> {
                cancelacion.setDecisionSoporte(aprobar ? "APROBADO" : "RECHAZADO");
                cancelacion.setEstadoTicket(aprobar ? "APROBADO" : "RECHAZADO");
            }
        }

        CancelacionProveedorEntity guardada = cancelacionRepository.save(cancelacion);

        if (helpServiceClient != null && guardada.getTicketCodigo() != null) {
            helpServiceClient.actualizarEstadoTicketSoporte(
                    guardada.getTicketCodigo(),
                    "RESUELTO",
                    guardada.getAgenteSoporteId(),
                    request.justificacion()
            );
        }

        if (amonestacionGenerada == null) {
            amonestacionGenerada = amonestacionRepository.findByCancelacionId(guardada.getId()).orElse(null);
        }

        return toResponse(guardada, solicitud.getEstadoSolicitud(), amonestacionGenerada,
                "Resolución de soporte registrada exitosamente.");
    }

    /**
     * Tarea programada que detecta tickets de Categoría 2 (Fuerza mayor) cuyo plazo de 3 días calendario
     * haya vencido sin un soporte válido aprobado, aplicando amonestación GRAVE (7 días de restricción)
     * de forma idempotente (nunca duplica amonestaciones por el mismo evento).
     */
    @Override
    @Scheduled(fixedDelay = 60000)
    public void procesarTicketsFuerzaMayorVencidos() {
        if (cancelacionRepository == null) return;
        List<CancelacionProveedorEntity> pendientes = cancelacionRepository.findByAmonestacionGeneradaFalse();
        if (pendientes == null || pendientes.isEmpty()) return;

        LocalDateTime ahora = LocalDateTime.now();

        for (CancelacionProveedorEntity c : pendientes) {
            if (c.getCategoria() != CategoriaCancelacionProveedor.CATEGORIA_2_FUERZA_MAYOR) {
                continue;
            }
            if ("APROBADO".equalsIgnoreCase(c.getEstadoTicket())) {
                continue;
            }
            if (c.getFechaLimiteEvidencia() != null && c.getFechaLimiteEvidencia().isBefore(ahora)) {
                c.setEstadoTicket("VENCIDO");
                c.setDecisionSoporte("VENCIDO_SIN_SOPORTE_VALIDO");
                c.setEsInjustificada(true);
                c.setFechaResolucion(ahora);
                c.setUpdatedAt(ahora);
                c = cancelacionRepository.save(c);

                PerfilProveedor proveedor = proveedorService != null
                        ? proveedorService.findById(c.getPerfilProveedorId()).orElse(null)
                        : null;

                generarAmonestacionGraveFuerzaMayorVencida(c, proveedor, ahora);
            }
        }
    }

    /**
     * Tarea programada que levanta automáticamente las restricciones cuya fecha de fin ya se cumplió,
     * notificando al proveedor que recupera la posibilidad de recibir ofertas.
     */
    @Override
    @Scheduled(fixedDelay = 60000)
    public void finalizarRestriccionesVencidas() {
        if (amonestacionRepository == null) return;
        List<AmonestacionProveedorEntity> activas = amonestacionRepository.findByActivaTrue();
        if (activas == null || activas.isEmpty()) return;

        LocalDateTime ahora = LocalDateTime.now();
        for (AmonestacionProveedorEntity a : activas) {
            if (a.getRestriccionFin() == null || !a.getRestriccionFin().isAfter(ahora)) {
                a.setActiva(false);
                amonestacionRepository.save(a);

                if (a.getDiasRestriccion() != null && a.getDiasRestriccion() > 0 && proveedorService != null) {
                    PerfilProveedor proveedor = proveedorService.findById(a.getPerfilProveedorId()).orElse(null);
                    if (proveedor != null && !tieneRestriccionVigente(proveedor.getId())) {
                        enviarNotificacionProveedor(proveedor, NotificationTemplates.RESTRICCION_FINALIZADA, Map.of(
                                "proveedorId", String.valueOf(proveedor.getId()),
                                "amonestacionId", String.valueOf(a.getId()),
                                "tipoEvento", "RESTRICCION_FINALIZADA"
                        ));
                    }
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneRestriccionVigente(Long proveedorId) {
        if (proveedorId == null || amonestacionRepository == null) return false;
        LocalDateTime ahora = LocalDateTime.now();
        return amonestacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(proveedorId).stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getActiva())
                        && a.getRestriccionFin() != null
                        && a.getRestriccionFin().isAfter(ahora));
    }

    @Override
    @Transactional(readOnly = true)
    public void validarProveedorSinRestriccionVigente(Long proveedorId) {
        if (proveedorId == null || amonestacionRepository == null) return;
        LocalDateTime ahora = LocalDateTime.now();
        amonestacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(proveedorId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getActiva())
                        && a.getRestriccionFin() != null
                        && a.getRestriccionFin().isAfter(ahora))
                .max(Comparator.comparing(AmonestacionProveedorEntity::getRestriccionFin))
                .ifPresent(restriccion -> {
                    long diasRestantes = Math.max(1L, Duration.between(ahora, restriccion.getRestriccionFin()).toDays() + 1);
                    throw new IllegalStateException(
                            "Tu cuenta tiene una restricción vigente por amonestación " + restriccion.getNivel()
                                    + " hasta " + restriccion.getRestriccionFin()
                                    + " (" + diasRestantes + " día(s) restante(s)). No puedes recibir ni enviar nuevas ofertas."
                    );
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CancelacionProveedorResponse obtenerCancelacionPorId(Long cancelacionId) {
        CancelacionProveedorEntity entity = cancelacionRepository.findById(cancelacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancelación no encontrada con id: " + cancelacionId));
        SolicitudServicio sol = solicitudService.findById(entity.getSolicitudServicioId()).orElse(null);
        AmonestacionProveedorEntity amon = amonestacionRepository.findByCancelacionId(entity.getId()).orElse(null);
        return toResponse(entity, sol != null ? sol.getEstadoSolicitud() : null, amon, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CancelacionProveedorResponse obtenerCancelacionPorTicket(String ticketCodigo) {
        CancelacionProveedorEntity entity = cancelacionRepository.findByTicketCodigo(ticketCodigo)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cancelación asociada al ticket: " + ticketCodigo));
        SolicitudServicio sol = solicitudService.findById(entity.getSolicitudServicioId()).orElse(null);
        AmonestacionProveedorEntity amon = amonestacionRepository.findByCancelacionId(entity.getId()).orElse(null);
        return toResponse(entity, sol != null ? sol.getEstadoSolicitud() : null, amon, null);
    }

    @Override
    @Transactional(readOnly = true)
    public HistorialDisciplinarioProveedorResponse obtenerHistorialDisciplinario(Long proveedorId) {
        proveedorService.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + proveedorId));

        LocalDateTime ahora = LocalDateTime.now();
        int cancelaciones30d = contarCancelacionesInjustificadasUltimos30Dias(proveedorId);
        int amonestaciones30d = contarAmonestacionesUltimos30Dias(proveedorId);
        NivelAmonestacion nivelActual = cancelaciones30d > 0
                ? NivelAmonestacion.porConteoInjustificadas(cancelaciones30d)
                : null;

        List<AmonestacionProveedorEntity> amonEntities = amonestacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(proveedorId);
        AmonestacionProveedorEntity restriccionActiva = amonEntities.stream()
                .filter(a -> Boolean.TRUE.equals(a.getActiva())
                        && a.getRestriccionFin() != null
                        && a.getRestriccionFin().isAfter(ahora))
                .max(Comparator.comparing(AmonestacionProveedorEntity::getRestriccionFin))
                .orElse(null);

        boolean tieneRestriccion = restriccionActiva != null;
        long diasRestantes = tieneRestriccion
                ? Math.max(1L, (Duration.between(ahora, restriccionActiva.getRestriccionFin()).toHours() + 23) / 24)
                : 0L;
        long horasRestantes = tieneRestriccion
                ? Math.max(1L, Duration.between(ahora, restriccionActiva.getRestriccionFin()).toHours())
                : 0L;

        List<AmonestacionProveedorResponse> amonResponses = amonEntities.stream()
                .map(a -> toAmonestacionResponse(a, ahora))
                .toList();

        List<CancelacionProveedorResponse> cancResponses = cancelacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(proveedorId).stream()
                .map(c -> {
                    SolicitudServicio sol = solicitudService.findById(c.getSolicitudServicioId()).orElse(null);
                    AmonestacionProveedorEntity amon = amonestacionRepository.findByCancelacionId(c.getId()).orElse(null);
                    return toResponse(c, sol != null ? sol.getEstadoSolicitud() : null, amon, null);
                })
                .toList();

        return new HistorialDisciplinarioProveedorResponse(
                proveedorId,
                cancelaciones30d,
                amonestaciones30d,
                nivelActual,
                tieneRestriccion,
                tieneRestriccion ? restriccionActiva.getRestriccionInicio() : null,
                tieneRestriccion ? restriccionActiva.getRestriccionFin() : null,
                diasRestantes,
                horasRestantes,
                amonResponses,
                cancResponses
        );
    }

    private AmonestacionProveedorEntity generarAmonestacionSegunEscala30Dias(
            CancelacionProveedorEntity cancelacion,
            PerfilProveedor proveedor,
            LocalDateTime ahora
    ) {
        // Garantizar idempotencia: una misma cancelación jamás genera más de una amonestación
        Optional<AmonestacionProveedorEntity> existente = amonestacionRepository.findByCancelacionId(cancelacion.getId());
        if (existente.isPresent()) {
            return existente.get();
        }

        int conteo30Dias = contarCancelacionesInjustificadasUltimos30Dias(cancelacion.getPerfilProveedorId());
        if (conteo30Dias <= 0) {
            conteo30Dias = 1;
        }

        NivelAmonestacion nivel = NivelAmonestacion.porConteoInjustificadas(conteo30Dias);
        int diasRestriccion = nivel.getDiasRestriccion();
        LocalDateTime inicioRestriccion = diasRestriccion > 0 ? ahora : null;
        LocalDateTime finRestriccion = diasRestriccion > 0 ? ahora.plusDays(diasRestriccion) : null;

        AmonestacionProveedorEntity amonestacion = AmonestacionProveedorEntity.builder()
                .perfilProveedorId(cancelacion.getPerfilProveedorId())
                .solicitudServicioId(cancelacion.getSolicitudServicioId())
                .cancelacionId(cancelacion.getId())
                .ticketCodigo(cancelacion.getTicketCodigo())
                .nivel(nivel)
                .motivo("Cancelación injustificada (" + cancelacion.getCategoria().name() + "): " + cancelacion.getMotivo())
                .conteo30Dias(conteo30Dias)
                .diasRestriccion(diasRestriccion)
                .restriccionInicio(inicioRestriccion)
                .restriccionFin(finRestriccion)
                .activa(diasRestriccion > 0)
                .createdAt(ahora)
                .build();

        AmonestacionProveedorEntity guardada = amonestacionRepository.save(amonestacion);

        cancelacion.setAmonestacionGenerada(true);
        cancelacionRepository.save(cancelacion);

        enviarNotificacionProveedor(proveedor, NotificationTemplates.AMONESTACION_GENERADA, Map.of(
                "solicitudId", String.valueOf(cancelacion.getSolicitudServicioId()),
                "amonestacionId", String.valueOf(guardada.getId()),
                "nivel", nivel.name(),
                "conteo30Dias", String.valueOf(conteo30Dias),
                "diasRestriccion", String.valueOf(diasRestriccion),
                "tipoEvento", "AMONESTACION_GENERADA"
        ));

        if (diasRestriccion > 0) {
            enviarNotificacionProveedor(proveedor, NotificationTemplates.RESTRICCION_ACTIVADA, Map.of(
                    "proveedorId", String.valueOf(cancelacion.getPerfilProveedorId()),
                    "nivel", nivel.name(),
                    "diasRestriccion", String.valueOf(diasRestriccion),
                    "restriccionFin", finRestriccion.toString(),
                    "tipoEvento", "RESTRICCION_ACTIVADA"
            ));
        }

        return guardada;
    }

    private AmonestacionProveedorEntity generarAmonestacionGraveFuerzaMayorVencida(
            CancelacionProveedorEntity cancelacion,
            PerfilProveedor proveedor,
            LocalDateTime ahora
    ) {
        Optional<AmonestacionProveedorEntity> existente = amonestacionRepository.findByCancelacionId(cancelacion.getId());
        if (existente.isPresent()) {
            return existente.get();
        }

        int conteo30Dias = contarCancelacionesInjustificadasUltimos30Dias(cancelacion.getPerfilProveedorId());
        int diasRestriccion = NivelAmonestacion.GRAVE.getDiasRestriccion(); // 7 días directos
        LocalDateTime finRestriccion = ahora.plusDays(diasRestriccion);

        AmonestacionProveedorEntity amonestacion = AmonestacionProveedorEntity.builder()
                .perfilProveedorId(cancelacion.getPerfilProveedorId())
                .solicitudServicioId(cancelacion.getSolicitudServicioId())
                .cancelacionId(cancelacion.getId())
                .ticketCodigo(cancelacion.getTicketCodigo())
                .nivel(NivelAmonestacion.GRAVE)
                .motivo("Fuerza mayor sin soporte válido tras vencimiento del plazo de 3 días: " + cancelacion.getMotivo())
                .conteo30Dias(Math.max(1, conteo30Dias))
                .diasRestriccion(diasRestriccion)
                .restriccionInicio(ahora)
                .restriccionFin(finRestriccion)
                .activa(true)
                .createdAt(ahora)
                .build();

        AmonestacionProveedorEntity guardada = amonestacionRepository.save(amonestacion);

        cancelacion.setAmonestacionGenerada(true);
        cancelacionRepository.save(cancelacion);

        enviarNotificacionProveedor(proveedor, NotificationTemplates.AMONESTACION_GENERADA, Map.of(
                "solicitudId", String.valueOf(cancelacion.getSolicitudServicioId()),
                "amonestacionId", String.valueOf(guardada.getId()),
                "ticketCodigo", cancelacion.getTicketCodigo() != null ? cancelacion.getTicketCodigo() : "",
                "nivel", NivelAmonestacion.GRAVE.name(),
                "diasRestriccion", String.valueOf(diasRestriccion),
                "motivo", "VENCIMIENTO_SOPORTE_FUERZA_MAYOR_3_DIAS",
                "tipoEvento", "AMONESTACION_GENERADA"
        ));
        enviarNotificacionProveedor(proveedor, NotificationTemplates.RESTRICCION_ACTIVADA, Map.of(
                "proveedorId", String.valueOf(cancelacion.getPerfilProveedorId()),
                "nivel", NivelAmonestacion.GRAVE.name(),
                "diasRestriccion", String.valueOf(diasRestriccion),
                "restriccionFin", finRestriccion.toString(),
                "tipoEvento", "RESTRICCION_ACTIVADA"
        ));

        return guardada;
    }

    private int contarCancelacionesInjustificadasUltimos30Dias(Long proveedorId) {
        if (cancelacionRepository == null || proveedorId == null) return 0;
        LocalDateTime corte = LocalDateTime.now().minusDays(VENTANA_MOVIL_DIAS);
        return (int) cancelacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(proveedorId).stream()
                .filter(c -> c.getCategoria() != CategoriaCancelacionProveedor.CATEGORIA_1_CLIENTE)
                .filter(c -> Boolean.TRUE.equals(c.getEsInjustificada()))
                .filter(c -> c.getCreatedAt() != null && !c.getCreatedAt().isBefore(corte))
                .count();
    }

    private int contarAmonestacionesUltimos30Dias(Long proveedorId) {
        if (amonestacionRepository == null || proveedorId == null) return 0;
        LocalDateTime corte = LocalDateTime.now().minusDays(VENTANA_MOVIL_DIAS);
        return (int) amonestacionRepository.findByPerfilProveedorIdOrderByCreatedAtDesc(proveedorId).stream()
                .filter(a -> a.getCreatedAt() != null && !a.getCreatedAt().isBefore(corte))
                .count();
    }

    private void enviarNotificacionProveedor(PerfilProveedor proveedor, String template, Map<String, String> params) {
        if (notificationPort == null || proveedor == null || proveedor.getId() == null) return;
        notificationPort.send("SERVICE_UPDATE", "PROVEEDOR_" + proveedor.getId(), template, "es", params);
        if (proveedor.getAuthUserId() != null && !proveedor.getAuthUserId().equals(proveedor.getId())) {
            notificationPort.send("SERVICE_UPDATE", "PROVEEDOR_" + proveedor.getAuthUserId(), template, "es", params);
        }
    }

    private void enviarNotificacionCliente(PerfilCliente cliente, Long clienteId, String template, Map<String, String> params) {
        if (notificationPort == null) return;
        String recipient = (cliente != null && cliente.getUsername() != null)
                ? cliente.getUsername()
                : ("CLIENTE_" + (clienteId != null ? clienteId : "0"));
        notificationPort.send("SERVICE_UPDATE", recipient, template, "es", params);
    }

    private CancelacionProveedorResponse toResponse(
            CancelacionProveedorEntity e,
            String estadoSolicitudActual,
            AmonestacionProveedorEntity amon,
            String mensajeUsuario
    ) {
        LocalDateTime ahora = LocalDateTime.now();
        return new CancelacionProveedorResponse(
                e.getId(),
                e.getSolicitudServicioId(),
                e.getOfertaId(),
                e.getPerfilProveedorId(),
                e.getCategoria(),
                e.getMotivo(),
                e.getDescripcion(),
                deserializeList(e.getEvidenciasUrls()),
                e.getTicketCodigo(),
                e.getEstadoCancelacion(),
                e.getEstadoTicket(),
                estadoSolicitudActual,
                e.getEsInjustificada(),
                e.getFechaLimiteEvidencia(),
                e.getDecisionSoporte(),
                e.getJustificacionSoporte(),
                e.getAgenteSoporteId(),
                e.getFechaResolucion(),
                e.getAmonestacionGenerada(),
                amon != null ? toAmonestacionResponse(amon, ahora) : null,
                mensajeUsuario,
                e.getCreatedAt()
        );
    }

    private AmonestacionProveedorResponse toAmonestacionResponse(AmonestacionProveedorEntity a, LocalDateTime ahora) {
        boolean vigente = Boolean.TRUE.equals(a.getActiva())
                && a.getRestriccionFin() != null
                && a.getRestriccionFin().isAfter(ahora);
        long diasRestantes = vigente
                ? Math.max(1L, (Duration.between(ahora, a.getRestriccionFin()).toHours() + 23) / 24)
                : 0L;

        return new AmonestacionProveedorResponse(
                a.getId(),
                a.getPerfilProveedorId(),
                a.getSolicitudServicioId(),
                a.getCancelacionId(),
                a.getTicketCodigo(),
                a.getNivel(),
                a.getMotivo(),
                a.getConteo30Dias(),
                a.getDiasRestriccion(),
                a.getRestriccionInicio(),
                a.getRestriccionFin(),
                vigente,
                diasRestantes,
                a.getCreatedAt()
        );
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return JSON_MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return JSON_MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}

