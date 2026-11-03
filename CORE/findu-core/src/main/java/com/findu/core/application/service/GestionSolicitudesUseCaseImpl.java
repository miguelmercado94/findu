package com.findu.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.core.application.dto.request.CrearSolicitudRequest;
import com.findu.core.application.dto.request.IniciarServicioRequest;
import com.findu.core.application.dto.request.ModificarSolicitudRequest;
import com.findu.core.application.dto.response.SolicitudResponse;
import com.findu.core.application.port.in.GestionCancelacionesProveedorUseCase;
import com.findu.core.application.port.in.GestionOfertasUseCase;
import com.findu.core.application.port.in.GestionSolicitudesUseCase;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.Direccion;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.domain.observer.SolicitudEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GestionSolicitudesUseCaseImpl implements GestionSolicitudesUseCase {

    private static final Set<String> ESTADOS_MODIFICABLES = Set.of("ABIERTA", "EN_NEGOCIACION");
    private static final Set<String> ESTADOS_CANCELABLES = Set.of("ABIERTA", "EN_NEGOCIACION", "PROGRAMADA", "ACEPTADA", "CANCELACION_RECHAZADA");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final SolicitudServicioService solicitudService;
    private final PerfilProveedorService proveedorService;
    private final PerfilClienteService perfilClienteService;
    private final DireccionService direccionService;
    private final OfertaService ofertaService;
    private final GestionOfertasUseCase ofertasUseCase;
    private final GestionCancelacionesProveedorUseCase cancelacionesUseCase;
    private final PinSeguridadHelper pinSeguridadHelper;
    private final SolicitudEstadoAuditoriaService auditoriaService;
    private final SolicitudEventPublisher eventPublisher;
    private final com.findu.core.application.port.out.persistence.ServicioRepositoryPort servicioPort;
    private final com.findu.core.application.port.out.persistence.CategoriaRepositoryPort categoriaPort;
    private final com.findu.core.application.port.out.persistence.MunicipioRepositoryPort municipioPort;
    private final com.findu.core.infrastructure.adapter.out.externalapi.TransactionServiceClient transactionServiceClient;

    @Override
    public SolicitudResponse crearSolicitud(CrearSolicitudRequest request) {
        SolicitudServicio solicitud = SolicitudServicio.builder()
                .perfilClienteId(request.perfilClienteId())
                .servicioId(request.servicioId())
                .direccionId(request.direccionId())
                .fechaProgramada(request.fechaProgramada())
                .nombreContacto(request.nombreContacto())
                .telefonoContacto(request.telefonoContacto())
                .prioridad(request.prioridad() != null ? request.prioridad() : 3)
                .presupuestoMaximo(request.presupuestoMaximo())
                .esPresupuestoEstricto(request.esPresupuestoEstricto() != null ? request.esPresupuestoEstricto() : false)
                .cantidadEstimada(request.cantidadEstimada() != null ? request.cantidadEstimada() : 1)
                .detalles(request.detalles())
                .fotos(request.fotos())
                .estadoSolicitud("ABIERTA")
                .recordatorio1hEnviado(false)
                .build();

        SolicitudServicio saved = solicitudService.save(solicitud);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    saved.getId(),
                    null,
                    "ABIERTA",
                    "CLIENTE",
                    request.perfilClienteId(),
                    null,
                    null,
                    null,
                    "Creación inicial de solicitud de servicio"
            );
        }

        // Filter online providers matching coverage area & specialty, excluding restricted providers
        Long municipioId = null;
        if (request.direccionId() != null) {
            municipioId = direccionService.findById(request.direccionId())
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }

        List<PerfilProveedor> proveedoresElegibles = filtrarProveedoresSinRestriccion(
                proveedorService.findEligibleProviders(request.servicioId(), municipioId)
        );
        eventPublisher.notifySolicitudCreada(saved, proveedoresElegibles);

        return toResponse(saved, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SolicitudResponse> consultarHistorial(Long clienteId, String estado, Pageable pageable) {
        if (estado != null && !estado.isBlank()) {
            return solicitudService.findByClienteIdAndEstado(clienteId, estado.toUpperCase(), pageable)
                    .map(s -> toResponse(s, true));
        }
        return solicitudService.findByClienteId(clienteId, pageable)
                .map(s -> toResponse(s, true));
    }

    @Override
    public SolicitudResponse modificarSolicitud(Long id, ModificarSolicitudRequest request) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        if (!ESTADOS_MODIFICABLES.contains(solicitud.getEstadoSolicitud())) {
            throw new IllegalStateException("No se puede modificar una solicitud en estado: " + solicitud.getEstadoSolicitud());
        }

        String estadoAnterior = solicitud.getEstadoSolicitud();

        // 1. Capturar valores anteriores para detectar exactamente qué cambió
        Long oldDireccionId = solicitud.getDireccionId();
        String oldDireccionTexto = oldDireccionId != null
                ? direccionService.findById(oldDireccionId).map(Direccion::getDireccionTexto).orElse(null)
                : null;
        LocalDateTime oldFechaProgramada = solicitud.getFechaProgramada();
        String oldNombreContacto = solicitud.getNombreContacto();
        String oldTelefonoContacto = solicitud.getTelefonoContacto();
        Integer oldPrioridad = solicitud.getPrioridad();
        BigDecimal oldPresupuestoMaximo = solicitud.getPresupuestoMaximo();
        Boolean oldEsPresupuestoEstricto = solicitud.getEsPresupuestoEstricto();
        String oldDetalles = solicitud.getDetalles();
        String oldFotos = solicitud.getFotos();

        List<String> camposModificados = new ArrayList<>();
        Map<String, Object> valoresAnteriores = new LinkedHashMap<>();

        // 2. Aplicar modificaciones y registrar diff
        if (request.direccionId() != null && !Objects.equals(request.direccionId(), oldDireccionId)) {
            solicitud.setDireccionId(request.direccionId());
            camposModificados.add("direccionId");
            camposModificados.add("direccionTexto");
            valoresAnteriores.put("direccionId", oldDireccionId);
            valoresAnteriores.put("direccionTexto", oldDireccionTexto);
        }

        boolean cambioFechaHora = false;
        if (request.fechaProgramada() != null && !Objects.equals(request.fechaProgramada(), oldFechaProgramada)) {
            solicitud.setFechaProgramada(request.fechaProgramada());
            cambioFechaHora = true;
            camposModificados.add("fechaProgramada");
            valoresAnteriores.put("fechaProgramada", oldFechaProgramada != null ? oldFechaProgramada.toString() : null);
            if (oldFechaProgramada == null || !Objects.equals(oldFechaProgramada.toLocalDate(), request.fechaProgramada().toLocalDate())) {
                camposModificados.add("fechaInicio");
                valoresAnteriores.put("fechaInicio", oldFechaProgramada != null ? oldFechaProgramada.toLocalDate().toString() : null);
            }
            if (oldFechaProgramada == null || !Objects.equals(oldFechaProgramada.toLocalTime(), request.fechaProgramada().toLocalTime())) {
                camposModificados.add("horaInicio");
                valoresAnteriores.put("horaInicio", oldFechaProgramada != null ? oldFechaProgramada.toLocalTime().toString() : null);
            }
        }

        if (request.nombreContacto() != null && !Objects.equals(request.nombreContacto(), oldNombreContacto)) {
            solicitud.setNombreContacto(request.nombreContacto());
            camposModificados.add("nombreContacto");
            valoresAnteriores.put("nombreContacto", oldNombreContacto);
        }

        if (request.telefonoContacto() != null && !Objects.equals(request.telefonoContacto(), oldTelefonoContacto)) {
            solicitud.setTelefonoContacto(request.telefonoContacto());
            camposModificados.add("telefonoContacto");
            valoresAnteriores.put("telefonoContacto", oldTelefonoContacto);
        }

        if (request.prioridad() != null && !Objects.equals(request.prioridad(), oldPrioridad)) {
            solicitud.setPrioridad(request.prioridad());
            camposModificados.add("prioridad");
            valoresAnteriores.put("prioridad", oldPrioridad);
        }

        if (!bigDecimalEquals(request.presupuestoMaximo(), oldPresupuestoMaximo)) {
            solicitud.setPresupuestoMaximo(request.presupuestoMaximo());
            camposModificados.add("presupuestoMaximo");
            valoresAnteriores.put("presupuestoMaximo", oldPresupuestoMaximo);
        }

        if (request.esPresupuestoEstricto() != null && !Objects.equals(request.esPresupuestoEstricto(), oldEsPresupuestoEstricto)) {
            solicitud.setEsPresupuestoEstricto(request.esPresupuestoEstricto());
            camposModificados.add("esPresupuestoEstricto");
            valoresAnteriores.put("esPresupuestoEstricto", oldEsPresupuestoEstricto);
        }

        if (request.detalles() != null && !Objects.equals(request.detalles(), oldDetalles)) {
            solicitud.setDetalles(request.detalles());
            camposModificados.add("detalles");
            valoresAnteriores.put("detalles", oldDetalles);
        }

        if (request.fotos() != null && !Objects.equals(request.fotos(), oldFotos)) {
            solicitud.setFotos(request.fotos());
            camposModificados.add("fotos");
            valoresAnteriores.put("fotos", oldFotos);
        }

        if (camposModificados.isEmpty()) {
            camposModificados.add("solicitud");
        }

        // Al modificar la solicitud, se limpian todas las ofertas de cara al cliente (vuelve a ABIERTA)
        solicitud.setEstadoSolicitud("ABIERTA");
        SolicitudServicio updated = solicitudService.save(solicitud);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    updated.getId(),
                    estadoAnterior,
                    "ABIERTA",
                    "CLIENTE",
                    updated.getPerfilClienteId(),
                    null,
                    null,
                    null,
                    "Modificación de solicitud por el cliente: " + camposModificados
            );
        }

        List<Oferta> ofertasExistentes = ofertaService.findBySolicitudId(updated.getId());
        List<Oferta> ofertasActivasParaReofertar = new ArrayList<>();
        Set<Long> proveedorIdsOfertaVencida = new HashSet<>();
        Set<Long> proveedorIdsOfertaActiva = new HashSet<>();

        String camposJson = serializeJson(camposModificados);
        String valoresJson = serializeJson(valoresAnteriores);

        for (Oferta o : ofertasExistentes) {
            if (esOfertaVencidaOInactiva(o)) {
                if (o.getPerfilProveedorId() != null) {
                    proveedorIdsOfertaVencida.add(o.getPerfilProveedorId());
                }
                ofertaService.deleteById(o.getId());
                continue;
            }

            boolean solapa = false;
            if (ofertasUseCase != null) {
                solapa = ofertasUseCase.verificarSolapamientoHorario(o.getPerfilProveedorId(), updated, o);
                if (!solapa && cambioFechaHora) {
                    Oferta ofertaConHoraCliente = Oferta.builder()
                            .id(o.getId())
                            .solicitudServicioId(updated.getId())
                            .perfilProveedorId(o.getPerfilProveedorId())
                            .horaInicioPropuesta(null)
                            .duracionUnidad(o.getDuracionUnidad())
                            .duracionCantidad(o.getDuracionCantidad())
                            .cronogramaDias(o.getCronogramaDias())
                            .build();
                    solapa = ofertasUseCase.verificarSolapamientoHorario(o.getPerfilProveedorId(), updated, ofertaConHoraCliente);
                }
            }

            o.setEstadoOferta("PENDIENTE_REOFERTA");
            o.setSolicitudModificada(true);
            o.setCamposModificados(camposJson);
            o.setValoresAnteriores(valoresJson);
            o.setSolapamientoHorario(solapa);

            Oferta guardada = ofertaService.save(o);
            ofertasActivasParaReofertar.add(guardada);
            if (guardada.getPerfilProveedorId() != null) {
                proveedorIdsOfertaActiva.add(guardada.getPerfilProveedorId());
            }
        }

        // Compute coverage changes and notify observers (excluding restricted providers)
        Long oldMunicipioId = null;
        if (oldDireccionId != null) {
            oldMunicipioId = direccionService.findById(oldDireccionId)
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }
        Long newMunicipioId = null;
        if (updated.getDireccionId() != null) {
            newMunicipioId = direccionService.findById(updated.getDireccionId())
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }

        List<PerfilProveedor> oldEligible = proveedorService.findEligibleProviders(updated.getServicioId(), oldMunicipioId);
        List<PerfilProveedor> newEligible = filtrarProveedoresSinRestriccion(
                proveedorService.findEligibleProviders(updated.getServicioId(), newMunicipioId)
        );

        Set<Long> newEligibleIds = newEligible.stream().map(PerfilProveedor::getId).collect(Collectors.toSet());
        List<PerfilProveedor> proveedoresRemovidos = oldEligible.stream()
                .filter(p -> !newEligibleIds.contains(p.getId()))
                .toList();

        List<PerfilProveedor> proveedoresComoNuevaSolicitud = newEligible.stream()
                .filter(p -> proveedorIdsOfertaVencida.contains(p.getId()))
                .toList();

        List<PerfilProveedor> proveedoresSinOfertaActualizados = newEligible.stream()
                .filter(p -> !proveedorIdsOfertaActiva.contains(p.getId()) && !proveedorIdsOfertaVencida.contains(p.getId()))
                .toList();

        eventPublisher.notifySolicitudModificadaParaOferentes(
                updated,
                ofertasActivasParaReofertar,
                proveedoresComoNuevaSolicitud,
                proveedoresSinOfertaActualizados,
                proveedoresRemovidos
        );

        return toResponse(updated, true);
    }

    @Override
    public SolicitudResponse cancelarSolicitud(Long id) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        if (!ESTADOS_CANCELABLES.contains(solicitud.getEstadoSolicitud())) {
            throw new IllegalStateException("No se puede cancelar una solicitud en estado: " + solicitud.getEstadoSolicitud());
        }

        String estadoAnterior = solicitud.getEstadoSolicitud();

        if (("PROGRAMADA".equals(solicitud.getEstadoSolicitud()) || "ACEPTADA".equals(solicitud.getEstadoSolicitud())) &&
                solicitud.getFechaProgramada() != null &&
                solicitud.getFechaProgramada().isBefore(LocalDateTime.now().plusHours(8))) {
            solicitud.setEstadoSolicitud("CANCELADA_CON_PENALIDAD");
        } else {
            solicitud.setEstadoSolicitud("CANCELADA_SIN_PENALIDAD");
        }

        SolicitudServicio updated = solicitudService.save(solicitud);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    updated.getId(),
                    estadoAnterior,
                    updated.getEstadoSolicitud(),
                    "CLIENTE",
                    updated.getPerfilClienteId(),
                    null,
                    null,
                    null,
                    "Cancelación de solicitud ejecutada por el cliente"
            );
        }

        List<Oferta> ofertasExistentes = ofertaService.findBySolicitudId(updated.getId());
        for (Oferta o : ofertasExistentes) {
            if ("ENVIADA".equalsIgnoreCase(o.getEstadoOferta()) || "PENDIENTE_REOFERTA".equalsIgnoreCase(o.getEstadoOferta())) {
                o.setEstadoOferta("RECHAZADA");
                ofertaService.save(o);
            }
        }

        Long municipioId = null;
        if (updated.getDireccionId() != null) {
            municipioId = direccionService.findById(updated.getDireccionId())
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }
        List<PerfilProveedor> proveedoresNotificados = proveedorService.findEligibleProviders(updated.getServicioId(), municipioId);

        eventPublisher.notifySolicitudCancelada(updated, proveedoresNotificados, ofertasExistentes);

        return toResponse(updated, true);
    }

    @Override
    public SolicitudResponse iniciarServicio(Long id) {
        return iniciarServicio(id, null);
    }

    @Override
    public SolicitudResponse iniciarServicio(Long id, IniciarServicioRequest request) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        PinSeguridadHelper helper = pinSeguridadHelper != null ? pinSeguridadHelper : new PinSeguridadHelper();

        // 1. Validar PIN de 4 dígitos cuando se envía request o cuando la solicitud tiene PIN asignado
        if (request != null || (solicitud.getPinInicioEncrypted() != null && !solicitud.getPinInicioEncrypted().isBlank())) {
            String pinRecibido = request != null ? request.pin() : null;
            if (pinRecibido == null || pinRecibido.isBlank()) {
                throw new IllegalArgumentException("Debe ingresar el PIN de validación de 4 dígitos proporcionado por el cliente para iniciar el servicio.");
            }
            if (!helper.validarPin(pinRecibido, solicitud.getPinInicioEncrypted())) {
                throw new IllegalArgumentException("El PIN de 4 dígitos ingresado no es válido para iniciar el servicio.");
            }
        }

        // 2. Validar proximidad GPS (máximo 20 metros alrededor de la dirección del servicio)
        BigDecimal distanciaCalculadaMetros = null;
        Direccion direccionServicio = solicitud.getDireccionId() != null
                ? direccionService.findById(solicitud.getDireccionId()).orElse(null)
                : null;

        if (direccionServicio != null && direccionServicio.getLatitud() != null && direccionServicio.getLongitud() != null) {
            if (request == null || request.latitud() == null || request.longitud() == null) {
                throw new IllegalArgumentException("Se requieren las coordenadas GPS actuales (latitud y longitud) para validar que te encuentras a máximo 20 metros de la dirección del servicio.");
            }
            double dist = helper.calcularDistanciaMetros(
                    request.latitud(),
                    request.longitud(),
                    direccionServicio.getLatitud(),
                    direccionServicio.getLongitud()
            );
            distanciaCalculadaMetros = BigDecimal.valueOf(dist).setScale(2, RoundingMode.HALF_UP);
            if (dist > PinSeguridadHelper.DISTANCIA_MAXIMA_INICIO_METROS) {
                throw new IllegalStateException(
                        String.format(Locale.US,
                                "No puedes iniciar el servicio: debes estar a máximo %.0f metros alrededor de la dirección del servicio (distancia actual: %.2f metros).",
                                PinSeguridadHelper.DISTANCIA_MAXIMA_INICIO_METROS, dist)
                );
            }
        } else if (request != null && request.latitud() != null && request.longitud() != null) {
            distanciaCalculadaMetros = BigDecimal.ZERO;
        }

        // 3. Seguro final de no solapamiento horario antes de pasar a EN_CURSO
        List<Oferta> ofertas = ofertaService.findBySolicitudId(id);
        Oferta aceptada = ofertas.stream()
                .filter(o -> "ACEPTADA".equalsIgnoreCase(o.getEstadoOferta()))
                .findFirst()
                .orElse(null);

        if (aceptada != null && aceptada.getPerfilProveedorId() != null && ofertasUseCase != null) {
            ofertasUseCase.validarNoSolapamientoHorarioProveedor(aceptada.getPerfilProveedorId(), solicitud, aceptada);
        }

        String estadoAnterior = solicitud.getEstadoSolicitud();
        solicitud.iniciarServicio();
        SolicitudServicio updated = solicitudService.save(solicitud);

        // 4. Registrar auditoría interna de hora y posición GPS
        Long actorId = (request != null && request.actorId() != null)
                ? request.actorId()
                : (aceptada != null ? aceptada.getPerfilProveedorId() : null);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    updated.getId(),
                    estadoAnterior,
                    updated.getEstadoSolicitud(),
                    "PROVEEDOR",
                    actorId,
                    request != null ? request.latitud() : null,
                    request != null ? request.longitud() : null,
                    distanciaCalculadaMetros,
                    "Inicio de servicio validado con PIN de 4 dígitos y GPS"
            );
        }

        PerfilCliente cliente = solicitud.getPerfilClienteId() != null
                ? perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null)
                : null;

        PerfilProveedor proveedor = (aceptada != null && aceptada.getPerfilProveedorId() != null)
                ? proveedorService.findById(aceptada.getPerfilProveedorId()).orElse(null)
                : null;

        eventPublisher.notifyServicioIniciado(updated, cliente, proveedor);

        return toResponse(updated, false);
    }

    @Override
    public SolicitudResponse finalizarServicio(Long id) {
        return finalizarServicio(id, null, null);
    }

    @Override
    public SolicitudResponse finalizarServicio(Long id, BigDecimal latitud, BigDecimal longitud) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        String estadoAnterior = solicitud.getEstadoSolicitud();
        solicitud.finalizarServicio();
        SolicitudServicio updated = solicitudService.save(solicitud);

        List<Oferta> ofertas = ofertaService.findBySolicitudId(id);
        Oferta aceptada = ofertas.stream()
                .filter(o -> "ACEPTADA".equalsIgnoreCase(o.getEstadoOferta()))
                .findFirst()
                .orElse(null);

        Long providerId = aceptada != null ? aceptada.getPerfilProveedorId() : 1L;
        BigDecimal amount = (aceptada != null && aceptada.getValorPropuesto() != null)
                ? aceptada.getValorPropuesto()
                : (solicitud.getPresupuestoMaximo() != null ? solicitud.getPresupuestoMaximo() : BigDecimal.valueOf(100000));

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    updated.getId(),
                    estadoAnterior,
                    updated.getEstadoSolicitud(),
                    "PROVEEDOR",
                    providerId,
                    latitud,
                    longitud,
                    null,
                    "Finalización de servicio"
            );
        }

        if (transactionServiceClient != null) {
            transactionServiceClient.registrarCreditoServicio(updated, providerId, amount);
        }

        PerfilCliente cliente = solicitud.getPerfilClienteId() != null
                ? perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null)
                : null;

        PerfilProveedor proveedor = proveedorService.findById(providerId).orElse(null);

        eventPublisher.notifyServicioFinalizado(updated, cliente, proveedor);

        return toResponse(updated, false);
    }

    @Override
    public SolicitudResponse reabrirSolicitudCliente(Long id, BigDecimal latitud, BigDecimal longitud) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        String estadoActual = solicitud.getEstadoSolicitud() != null ? solicitud.getEstadoSolicitud().toUpperCase() : "";
        if (!estadoActual.startsWith("CANCELAD") && !"ABIERTA".equals(estadoActual)) {
            throw new IllegalStateException("Solo se puede reabrir una solicitud que haya sido cancelada previamente (estado actual: " + estadoActual + ")");
        }

        String estadoAnterior = solicitud.getEstadoSolicitud();
        solicitud.setEstadoSolicitud("ABIERTA");
        solicitud.setPinInicioEncrypted(null);
        solicitud.setRecordatorio1hEnviado(false);
        SolicitudServicio updated = solicitudService.save(solicitud);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    updated.getId(),
                    estadoAnterior,
                    "ABIERTA",
                    "CLIENTE",
                    updated.getPerfilClienteId(),
                    latitud,
                    longitud,
                    null,
                    "Solicitud reabierta por el cliente para buscar nuevo proveedor"
            );
        }

        Long municipioId = null;
        if (updated.getDireccionId() != null) {
            municipioId = direccionService.findById(updated.getDireccionId())
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }

        List<PerfilProveedor> proveedoresElegibles = filtrarProveedoresSinRestriccion(
                proveedorService.findEligibleProviders(updated.getServicioId(), municipioId)
        );
        eventPublisher.notifySolicitudCreada(updated, proveedoresElegibles);

        return toResponse(updated, true);
    }

    /**
     * Tarea programada que envía una notificación/alarma al proveedor 1 hora antes (<= 60 min)
     * de comenzar un servicio aceptado o programado.
     */
    @Override
    @Scheduled(fixedDelay = 60000)
    public void enviarAlarmasUnaHoraAntes() {
        if (solicitudService == null) return;

        List<SolicitudServicio> programadas = new ArrayList<>();
        List<SolicitudServicio> aceptadasList = solicitudService.findByEstado("ACEPTADA");
        if (aceptadasList != null) programadas.addAll(aceptadasList);
        List<SolicitudServicio> progList = solicitudService.findByEstado("PROGRAMADA");
        if (progList != null) programadas.addAll(progList);

        if (programadas.isEmpty()) return;

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limiteUnaHora = ahora.plusMinutes(60);

        for (SolicitudServicio s : programadas) {
            if (Boolean.TRUE.equals(s.getRecordatorio1hEnviado())) {
                continue;
            }
            if (s.getFechaProgramada() == null) {
                continue;
            }
            if (!s.getFechaProgramada().isBefore(ahora) && !s.getFechaProgramada().isAfter(limiteUnaHora)) {
                s.setRecordatorio1hEnviado(true);
                solicitudService.save(s);

                Oferta aceptada = ofertaService != null
                        ? ofertaService.findBySolicitudId(s.getId()).stream()
                                .filter(o -> "ACEPTADA".equalsIgnoreCase(o.getEstadoOferta()))
                                .findFirst()
                                .orElse(null)
                        : null;

                PerfilProveedor proveedor = (aceptada != null && aceptada.getPerfilProveedorId() != null && proveedorService != null)
                        ? proveedorService.findById(aceptada.getPerfilProveedorId()).orElse(null)
                        : null;

                if (eventPublisher != null) {
                    eventPublisher.notifyAlarmaServicio1Hora(s, aceptada, proveedor);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudResponse obtenerSolicitudPorId(Long id) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));
        // Por seguridad, obtenerSolicitudPorId genérico no expone el PIN a menos que se invoque obtenerSolicitudParaCliente
        return toResponse(solicitud, false);
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudResponse obtenerSolicitudParaCliente(Long id, Long clienteId) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));
        boolean esDueno = clienteId != null && clienteId.equals(solicitud.getPerfilClienteId());
        return toResponse(solicitud, esDueno);
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerPinInicioParaCliente(Long id, Long clienteId) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));
        if (clienteId == null || !clienteId.equals(solicitud.getPerfilClienteId())) {
            throw new IllegalStateException("Solo el cliente titular de la solicitud está autorizado para visualizar el PIN de inicio.");
        }
        PinSeguridadHelper helper = pinSeguridadHelper != null ? pinSeguridadHelper : new PinSeguridadHelper();
        return helper.desencriptarPin(solicitud.getPinInicioEncrypted());
    }

    private List<PerfilProveedor> filtrarProveedoresSinRestriccion(List<PerfilProveedor> proveedores) {
        if (proveedores == null || proveedores.isEmpty() || cancelacionesUseCase == null) {
            return proveedores != null ? proveedores : List.of();
        }
        return proveedores.stream()
                .filter(p -> p != null && p.getId() != null && !cancelacionesUseCase.tieneRestriccionVigente(p.getId()))
                .toList();
    }

    private boolean esOfertaVencidaOInactiva(Oferta o) {
        if (o == null) return true;
        String estado = o.getEstadoOferta();
        if ("EXPIRADA".equalsIgnoreCase(estado) || "RETIRADA".equalsIgnoreCase(estado) || "RECHAZADA".equalsIgnoreCase(estado)) {
            return true;
        }
        if ("ENVIADA".equalsIgnoreCase(estado)) {
            LocalDateTime ref = o.getOfertadoAt() != null ? o.getOfertadoAt() : o.getCreatedAt();
            if (ref != null) {
                long elapsedSeconds = Duration.between(ref, LocalDateTime.now()).getSeconds();
                return elapsedSeconds >= GestionOfertasUseCaseImpl.EXPIRACION_OFERTA_MINUTOS * 60L;
            }
        }
        return false;
    }

    private boolean bigDecimalEquals(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    private String serializeJson(Object value) {
        if (value == null) return null;
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private SolicitudResponse toResponse(SolicitudServicio s, boolean incluirPinCliente) {
        String servicioNombre = null;
        String categoriaNombre = null;
        if (s.getServicioId() != null) {
            var servOpt = servicioPort.findById(s.getServicioId());
            if (servOpt.isPresent()) {
                var serv = servOpt.get();
                servicioNombre = serv.getNombre();
                if (serv.getCategoriaId() != null) {
                    categoriaNombre = categoriaPort.findById(serv.getCategoriaId())
                            .map(com.findu.core.domain.model.Categoria::getNombre)
                            .orElse(null);
                }
            }
        }

        String direccionTexto = null;
        String municipioNombre = null;
        String codigoDane = null;
        if (s.getDireccionId() != null) {
            var dirOpt = direccionService.findById(s.getDireccionId());
            if (dirOpt.isPresent()) {
                var dir = dirOpt.get();
                direccionTexto = dir.getDireccionTexto();
                if (dir.getMunicipioId() != null) {
                    var munOpt = municipioPort.findById(dir.getMunicipioId());
                    if (munOpt.isPresent()) {
                        municipioNombre = munOpt.get().getNombre();
                        codigoDane = munOpt.get().getCodigoDane();
                    }
                }
            }
        }

        String nombreCliente = s.getNombreContacto();
        BigDecimal clienteCalificacion = BigDecimal.valueOf(5.0);
        if (s.getPerfilClienteId() != null) {
            var cliOpt = perfilClienteService.findById(s.getPerfilClienteId());
            if (cliOpt.isPresent()) {
                if (nombreCliente == null || nombreCliente.isBlank()) {
                    nombreCliente = cliOpt.get().getNombreCompleto();
                }
                if (cliOpt.get().getCalificacionPromedio() != null) {
                    clienteCalificacion = cliOpt.get().getCalificacionPromedio();
                }
            }
        }

        BigDecimal tarifaAcordada = null;
        List<Oferta> ofertas = ofertaService.findBySolicitudId(s.getId());
        for (Oferta o : ofertas) {
            if ("ACEPTADA".equalsIgnoreCase(o.getEstadoOferta())) {
                tarifaAcordada = o.getValorPropuesto();
                break;
            }
        }

        String pinPlano = null;
        if (incluirPinCliente && s.getPinInicioEncrypted() != null && !s.getPinInicioEncrypted().isBlank()) {
            PinSeguridadHelper helper = pinSeguridadHelper != null ? pinSeguridadHelper : new PinSeguridadHelper();
            pinPlano = helper.desencriptarPin(s.getPinInicioEncrypted());
        }

        return new SolicitudResponse(
                s.getId(),
                servicioNombre,
                categoriaNombre,
                direccionTexto,
                municipioNombre,
                codigoDane,
                2.0,
                s.getFechaProgramada(),
                nombreCliente != null ? nombreCliente : "Cliente FindU",
                s.getTelefonoContacto(),
                clienteCalificacion,
                s.getPrioridad(),
                s.getPresupuestoMaximo(),
                s.getEsPresupuestoEstricto(),
                tarifaAcordada,
                s.getDetalles(),
                s.getFotos(),
                s.getEstadoSolicitud(),
                null,
                null,
                pinPlano
        );
    }
}
