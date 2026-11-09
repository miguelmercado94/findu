package com.findu.core.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.core.application.dto.request.AproximacionDuracionRequest;
import com.findu.core.application.dto.request.CrearOfertaRequest;
import com.findu.core.application.dto.request.DiaTrabajoOfertaRequest;
import com.findu.core.application.dto.request.FranjaHorariaOfertaRequest;
import com.findu.core.application.dto.response.AproximacionDuracionResponse;
import com.findu.core.application.dto.response.DiaTrabajoOfertaResponse;
import com.findu.core.application.dto.response.OfertaProveedorDetalleResponse;
import com.findu.core.application.dto.response.OfertaResponse;
import com.findu.core.application.dto.response.SolicitudResponse;
import com.findu.core.application.port.in.GestionOfertasUseCase;
import com.findu.core.application.port.out.externalapi.NotificationPort;
import com.findu.core.application.port.out.persistence.CategoriaRepositoryPort;
import com.findu.core.application.port.out.persistence.MunicipioRepositoryPort;
import com.findu.core.application.port.out.persistence.ServicioRepositoryPort;
import com.findu.core.domain.enums.UnidadDuracionOferta;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.*;
import com.findu.core.domain.observer.SolicitudEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GestionOfertasUseCaseImpl implements GestionOfertasUseCase {

    public static final long EXPIRACION_OFERTA_MINUTOS = 10L;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final OfertaService ofertaService;
    private final SolicitudServicioService solicitudService;
    private final PerfilProveedorService proveedorService;
    private final PerfilClienteService perfilClienteService;
    private final DireccionService direccionService;
    private final NotificationPort notificationPort;
    private final SolicitudEventPublisher eventPublisher;
    private final ServicioRepositoryPort servicioPort;
    private final PerfilEspecialistaService perfilEspecialistaService;
    private final ProveedorCoberturaService proveedorCoberturaService;
    private final CategoriaRepositoryPort categoriaPort;
    private final MunicipioRepositoryPort municipioPort;
    private final CalificacionService calificacionService;
    private final com.findu.core.application.port.in.GestionCancelacionesProveedorUseCase cancelacionesUseCase;
    private final PinSeguridadHelper pinSeguridadHelper;
    private final SolicitudEstadoAuditoriaService auditoriaService;
    private final FotoUrlResolver fotoUrlResolver;

    @Override
    public OfertaResponse enviarOferta(CrearOfertaRequest request) {
        if (cancelacionesUseCase != null && request.perfilProveedorId() != null) {
            cancelacionesUseCase.validarProveedorSinRestriccionVigente(request.perfilProveedorId());
        }

        SolicitudServicio solicitud = solicitudService.findById(request.solicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        String estadoActual = solicitud.getEstadoSolicitud();
        if (estadoActual != null && !"ABIERTA".equalsIgnoreCase(estadoActual) && !"EN_NEGOCIACION".equalsIgnoreCase(estadoActual)) {
            throw new IllegalStateException("No se pueden enviar ofertas a una solicitud en estado: " + estadoActual);
        }

        // Si ya existe una oferta previa de este proveedor para esta solicitud:
        // - Si está en PENDIENTE_REOFERTA o RETIRADA, permitimos reenviarla actualizando ese registro.
        // - Si está en ENVIADA y ya cumplió > 10 min, la marcamos como EXPIRADA.
        Optional<Oferta> ofertaExistenteOpt = ofertaService != null
                ? ofertaService.findBySolicitudIdAndProveedorId(request.solicitudServicioId(), request.perfilProveedorId())
                : Optional.empty();

        if (ofertaExistenteOpt != null && ofertaExistenteOpt.isPresent()) {
            Oferta existente = ofertaExistenteOpt.get();
            if ("ENVIADA".equalsIgnoreCase(existente.getEstadoOferta()) && haExpiradoOferta(existente)) {
                existente.setEstadoOferta("EXPIRADA");
                ofertaService.save(existente);
            }
            if ("PENDIENTE_REOFERTA".equalsIgnoreCase(existente.getEstadoOferta())
                    || "RETIRADA".equalsIgnoreCase(existente.getEstadoOferta())
                    || "ENVIADA".equalsIgnoreCase(existente.getEstadoOferta())) {
                return actualizarOfertaInterna(existente, solicitud, request);
            }
            if ("EXPIRADA".equalsIgnoreCase(existente.getEstadoOferta())) {
                throw new IllegalStateException("Tu oferta anterior para esta solicitud expiró tras 10 minutos sin respuesta.");
            }
        } else if (ofertaService.existsBySolicitudIdAndProveedorId(request.solicitudServicioId(), request.perfilProveedorId())) {
            throw new IllegalStateException("Ya enviaste una oferta para esta solicitud.");
        }

        BigDecimal valorPropuesto = resolverYValidarValorPropuesto(request, solicitud);
        validarHoraPropuestaNoEnPasado(request.horaInicioPropuesta(), solicitud);

        // Procesar y validar la aproximación de duración (por defecto: 1 HORA)
        DuracionResuelta duracionResuelta = procesarYValidarDuracion(request.aproximacionDuracion());

        String tiempoEstimadoFinal = request.tiempoEstimado();
        if (tiempoEstimadoFinal == null || tiempoEstimadoFinal.isBlank()) {
            tiempoEstimadoFinal = generarResumenTiempoEstimado(duracionResuelta);
        }

        LocalDateTime ahora = LocalDateTime.now();

        Oferta oferta = Oferta.builder()
                .solicitudServicioId(request.solicitudServicioId())
                .perfilProveedorId(request.perfilProveedorId())
                .valorPropuesto(valorPropuesto)
                .horaInicioPropuesta(request.horaInicioPropuesta())
                .tiempoEstimado(tiempoEstimadoFinal)
                .mensajePresentacion(request.mensajePresentacion())
                .duracionUnidad(duracionResuelta.unidad())
                .duracionCantidad(duracionResuelta.cantidad())
                .mismoHorarioDias(duracionResuelta.mismoHorarioDias())
                .cronogramaDias(duracionResuelta.cronogramaDias())
                .ofertadoAt(ahora)
                .solicitudModificada(false)
                .camposModificados(null)
                .valoresAnteriores(null)
                .solapamientoHorario(false)
                .estadoOferta("ENVIADA")
                .build();

        // Validar que el horario propuesto no se solape con otros trabajos activos del proveedor
        validarNoSolapamientoHorarioProveedor(request.perfilProveedorId(), solicitud, oferta);

        // Delegate state transition logic to domain entity via State Pattern
        solicitud.recibirOferta(oferta);
        solicitudService.save(solicitud);

        Oferta saved = ofertaService.save(oferta);

        // Notify client and observers
        PerfilProveedor proveedor = proveedorService.findById(request.perfilProveedorId()).orElse(null);
        PerfilCliente cliente = perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null);

        eventPublisher.notifyOfertaRecibida(solicitud, saved, cliente, proveedor);

        return toResponse(saved);
    }

    @Override
    public OfertaResponse actualizarOferta(Long ofertaId, CrearOfertaRequest request) {
        Oferta existente = ofertaService.findById(ofertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada con id: " + ofertaId));

        if (request.perfilProveedorId() != null && !request.perfilProveedorId().equals(existente.getPerfilProveedorId())) {
            throw new IllegalStateException("No tienes permisos para editar una oferta de otro proveedor.");
        }

        if (!"ENVIADA".equalsIgnoreCase(existente.getEstadoOferta())
                && !"PENDIENTE_REOFERTA".equalsIgnoreCase(existente.getEstadoOferta())) {
            throw new IllegalStateException("Solo se pueden editar o reenviar ofertas en estado ENVIADA o PENDIENTE_REOFERTA. Estado actual: " + existente.getEstadoOferta());
        }

        SolicitudServicio solicitud = solicitudService.findById(existente.getSolicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        String estadoSolicitud = solicitud.getEstadoSolicitud();
        if (estadoSolicitud != null && !"ABIERTA".equalsIgnoreCase(estadoSolicitud) && !"EN_NEGOCIACION".equalsIgnoreCase(estadoSolicitud)) {
            throw new IllegalStateException("No se puede reenviar la oferta porque la solicitud ya está en estado: " + estadoSolicitud);
        }

        return actualizarOfertaInterna(existente, solicitud, request);
    }

    private OfertaResponse actualizarOfertaInterna(Oferta existente, SolicitudServicio solicitud, CrearOfertaRequest request) {
        BigDecimal valorPropuesto = resolverYValidarValorPropuesto(request, solicitud);
        validarHoraPropuestaNoEnPasado(request.horaInicioPropuesta(), solicitud);

        DuracionResuelta duracionResuelta = procesarYValidarDuracion(request.aproximacionDuracion());
        String tiempoEstimadoFinal = request.tiempoEstimado();
        if (tiempoEstimadoFinal == null || tiempoEstimadoFinal.isBlank()) {
            tiempoEstimadoFinal = generarResumenTiempoEstimado(duracionResuelta);
        }

        existente.setValorPropuesto(valorPropuesto);
        existente.setHoraInicioPropuesta(request.horaInicioPropuesta());
        existente.setTiempoEstimado(tiempoEstimadoFinal);
        existente.setMensajePresentacion(request.mensajePresentacion());
        existente.setDuracionUnidad(duracionResuelta.unidad());
        existente.setDuracionCantidad(duracionResuelta.cantidad());
        existente.setMismoHorarioDias(duracionResuelta.mismoHorarioDias());
        existente.setCronogramaDias(new ArrayList<>(duracionResuelta.cronogramaDias()));
        existente.setOfertadoAt(LocalDateTime.now());
        existente.setSolicitudModificada(false);
        existente.setCamposModificados(null);
        existente.setValoresAnteriores(null);
        existente.setSolapamientoHorario(false);
        existente.setEstadoOferta("ENVIADA");

        // Validar no solapamiento con los trabajos en curso / aceptados del proveedor
        validarNoSolapamientoHorarioProveedor(existente.getPerfilProveedorId(), solicitud, existente);

        solicitud.recibirOferta(existente);
        solicitudService.save(solicitud);

        Oferta saved = ofertaService.save(existente);

        PerfilProveedor proveedor = proveedorService.findById(existente.getPerfilProveedorId()).orElse(null);
        PerfilCliente cliente = perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null);
        eventPublisher.notifyOfertaRecibida(solicitud, saved, cliente, proveedor);

        return toResponse(saved);
    }

    @Override
    public void retirarOferta(Long ofertaId, Long proveedorId) {
        Oferta oferta = ofertaService.findById(ofertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada con id: " + ofertaId));

        if (proveedorId != null && !proveedorId.equals(oferta.getPerfilProveedorId())) {
            throw new IllegalStateException("No puedes retirar una oferta que pertenece a otro proveedor.");
        }

        if ("ACEPTADA".equalsIgnoreCase(oferta.getEstadoOferta())) {
            throw new IllegalStateException("No se puede retirar una oferta que ya fue ACEPTADA por el cliente.");
        }

        oferta.setEstadoOferta("RETIRADA");
        oferta.setSolicitudModificada(false);
        oferta.setSolapamientoHorario(false);
        ofertaService.save(oferta);

        SolicitudServicio solicitud = solicitudService.findById(oferta.getSolicitudServicioId()).orElse(null);
        if (solicitud != null) {
            boolean quedanOfertasActivas = ofertaService.findBySolicitudId(solicitud.getId()).stream()
                    .anyMatch(o -> !o.getId().equals(ofertaId)
                            && "ENVIADA".equalsIgnoreCase(o.getEstadoOferta())
                            && !haExpiradoOferta(o));
            if (!quedanOfertasActivas && "EN_NEGOCIACION".equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
                solicitud.setEstadoSolicitud("ABIERTA");
                solicitudService.save(solicitud);
            }

            PerfilProveedor proveedor = proveedorService.findById(oferta.getPerfilProveedorId()).orElse(null);
            PerfilCliente cliente = solicitud.getPerfilClienteId() != null
                    ? perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null)
                    : null;
            eventPublisher.notifyOfertaRetirada(solicitud, oferta, cliente, proveedor);
        }
    }

    private BigDecimal resolverYValidarValorPropuesto(CrearOfertaRequest request, SolicitudServicio solicitud) {
        if (request.valorPropuesto() == null && request.horaInicioPropuesta() == null) {
            throw new IllegalArgumentException("Debe proponer al menos un precio o una hora de inicio.");
        }

        BigDecimal valorPropuesto = request.valorPropuesto();
        if (valorPropuesto == null) {
            valorPropuesto = solicitud.getPresupuestoMaximo();
        }
        if (valorPropuesto == null) {
            throw new IllegalArgumentException("Debe especificar un valor propuesto ya que la solicitud no tiene presupuesto asignado.");
        }
        if (valorPropuesto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor propuesto debe ser mayor a cero.");
        }

        if (Boolean.TRUE.equals(solicitud.getEsPresupuestoEstricto()) &&
                solicitud.getPresupuestoMaximo() != null &&
                valorPropuesto.compareTo(solicitud.getPresupuestoMaximo()) > 0) {
            throw new IllegalStateException("El cliente ha fijado un tope máximo estricto ($" + solicitud.getPresupuestoMaximo() + "). Tu oferta ($" + valorPropuesto + ") excede este límite.");
        }
        return valorPropuesto;
    }

    private void validarHoraPropuestaNoEnPasado(LocalTime horaInicioPropuesta, SolicitudServicio solicitud) {
        if (horaInicioPropuesta != null && solicitud.getFechaProgramada() != null) {
            LocalDateTime fechaHoraPropuesta = solicitud.getFechaProgramada().toLocalDate().atTime(horaInicioPropuesta);
            if (fechaHoraPropuesta.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("La hora propuesta (" + horaInicioPropuesta + ") resulta en una fecha/hora en el pasado.");
            }
        }
    }

    public boolean haExpiradoOferta(Oferta oferta) {
        if (oferta == null) return false;
        if ("EXPIRADA".equalsIgnoreCase(oferta.getEstadoOferta())) return true;
        if (!"ENVIADA".equalsIgnoreCase(oferta.getEstadoOferta())) return false;
        LocalDateTime ref = oferta.getOfertadoAt() != null
                ? oferta.getOfertadoAt()
                : (oferta.getUpdatedAt() != null ? oferta.getUpdatedAt() : oferta.getCreatedAt());
        if (ref == null) return false;
        return ref.plusMinutes(EXPIRACION_OFERTA_MINUTOS).isBefore(LocalDateTime.now());
    }

    private void procesarExpiracionOferta(Oferta o) {
        o.setEstadoOferta("EXPIRADA");
        ofertaService.save(o);

        SolicitudServicio solicitud = solicitudService != null && o.getSolicitudServicioId() != null
                ? solicitudService.findById(o.getSolicitudServicioId()).orElse(null)
                : null;
        if (solicitud != null) {
            List<Oferta> todas = ofertaService.findBySolicitudId(solicitud.getId());
            boolean quedanActivas = todas != null && todas.stream()
                    .anyMatch(other -> !Objects.equals(other.getId(), o.getId())
                            && "ENVIADA".equalsIgnoreCase(other.getEstadoOferta())
                            && !haExpiradoOferta(other));
            if (!quedanActivas && "EN_NEGOCIACION".equalsIgnoreCase(solicitud.getEstadoSolicitud())) {
                solicitud.setEstadoSolicitud("ABIERTA");
                solicitudService.save(solicitud);
            }
            PerfilProveedor proveedor = proveedorService != null && o.getPerfilProveedorId() != null
                    ? proveedorService.findById(o.getPerfilProveedorId()).orElse(null)
                    : null;
            PerfilCliente cliente = perfilClienteService != null && solicitud.getPerfilClienteId() != null
                    ? perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null)
                    : null;
            if (eventPublisher != null) {
                eventPublisher.notifyOfertaExpirada(solicitud, o, cliente, proveedor);
            }
        }
    }

    @Scheduled(fixedDelay = 15000)
    public void expirarOfertasVencidas() {
        if (ofertaService == null) return;
        List<Oferta> enviadas = ofertaService.findByEstadoOferta("ENVIADA");
        if (enviadas == null || enviadas.isEmpty()) return;

        for (Oferta o : enviadas) {
            if (haExpiradoOferta(o)) {
                procesarExpiracionOferta(o);
            }
        }
    }

    private record IntervaloTiempo(LocalDateTime inicio, LocalDateTime fin, Long solicitudId) {}

    private List<IntervaloTiempo> calcularIntervalosOferta(SolicitudServicio solicitud, Oferta oferta) {
        List<IntervaloTiempo> intervalos = new ArrayList<>();
        if (solicitud == null) return intervalos;

        UnidadDuracionOferta unidad = oferta != null && oferta.getDuracionUnidad() != null
                ? oferta.getDuracionUnidad()
                : UnidadDuracionOferta.HORAS;
        BigDecimal cantidad = oferta != null && oferta.getDuracionCantidad() != null
                ? oferta.getDuracionCantidad()
                : BigDecimal.ONE;

        if (unidad == UnidadDuracionOferta.DIAS && oferta != null && oferta.getCronogramaDias() != null && !oferta.getCronogramaDias().isEmpty()) {
            for (OfertaCronogramaDia dia : oferta.getCronogramaDias()) {
                if (dia.getFecha() == null) continue;
                LocalTime hIni = dia.getHoraInicio() != null ? dia.getHoraInicio() : LocalTime.of(8, 0);
                LocalTime hFin = dia.getHoraFin();
                if (hFin == null) {
                    long mins = dia.getHorasTrabajo() != null
                            ? dia.getHorasTrabajo().multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).longValue()
                            : 60L;
                    hFin = hIni.plusMinutes(mins);
                }
                LocalDateTime dtIni = dia.getFecha().atTime(hIni);
                LocalDateTime dtFin = dia.getFecha().atTime(hFin);
                if (!dtFin.isAfter(dtIni)) {
                    dtFin = dtIni.plusHours(1);
                }
                intervalos.add(new IntervaloTiempo(dtIni, dtFin, solicitud.getId()));
            }
            return intervalos;
        }

        if (solicitud.getFechaProgramada() == null) {
            return intervalos;
        }

        LocalDateTime inicio = solicitud.getFechaProgramada();
        if (oferta != null && oferta.getHoraInicioPropuesta() != null) {
            inicio = solicitud.getFechaProgramada().toLocalDate().atTime(oferta.getHoraInicioPropuesta());
        }

        long duracionMinutos = switch (unidad) {
            case MINUTOS -> cantidad.setScale(0, RoundingMode.HALF_UP).longValue();
            case HORAS -> cantidad.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).longValue();
            case DIAS -> cantidad.multiply(BigDecimal.valueOf(8 * 60)).setScale(0, RoundingMode.HALF_UP).longValue();
        };
        if (duracionMinutos <= 0) duracionMinutos = 60L;

        intervalos.add(new IntervaloTiempo(inicio, inicio.plusMinutes(duracionMinutos), solicitud.getId()));
        return intervalos;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificarSolapamientoHorario(Long proveedorId, SolicitudServicio solicitudObjetivo, Oferta ofertaObjetivo) {
        return encontrarConflictoHorario(proveedorId, solicitudObjetivo, ofertaObjetivo).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public void validarNoSolapamientoHorarioProveedor(Long proveedorId, SolicitudServicio solicitudObjetivo, Oferta ofertaObjetivo) {
        encontrarConflictoHorario(proveedorId, solicitudObjetivo, ofertaObjetivo).ifPresent(conflicto -> {
            throw new IllegalStateException(
                    "Conflicto de horario en tu agenda: el horario propuesto se solapa con la solicitud activa #" + conflicto.getId()
                            + " programada para " + conflicto.getFechaProgramada() + "."
            );
        });
    }

    private Optional<SolicitudServicio> encontrarConflictoHorario(Long proveedorId, SolicitudServicio solicitudObjetivo, Oferta ofertaObjetivo) {
        if (proveedorId == null || solicitudObjetivo == null || solicitudService == null) {
            return Optional.empty();
        }

        List<IntervaloTiempo> intervalosNuevos = calcularIntervalosOferta(solicitudObjetivo, ofertaObjetivo);
        if (intervalosNuevos.isEmpty()) {
            return Optional.empty();
        }

        Map<Long, SolicitudServicio> solicitudesMap = new LinkedHashMap<>();
        Map<Long, Oferta> ofertasAceptadasMap = new HashMap<>();

        List<SolicitudServicio> misSolicitudesAceptadas = solicitudService.findByProveedorIdViaOfertaAceptada(proveedorId, null);
        if (misSolicitudesAceptadas != null) {
            for (SolicitudServicio s : misSolicitudesAceptadas) {
                if (s != null && s.getId() != null) {
                    solicitudesMap.put(s.getId(), s);
                }
            }
        }

        if (ofertaService != null) {
            List<Oferta> ofertasDelProveedor = ofertaService.findByProveedorId(proveedorId);
            if (ofertasDelProveedor != null) {
                for (Oferta o : ofertasDelProveedor) {
                    if (o != null && "ACEPTADA".equalsIgnoreCase(o.getEstadoOferta()) && o.getSolicitudServicioId() != null) {
                        ofertasAceptadasMap.put(o.getSolicitudServicioId(), o);
                        if (!solicitudesMap.containsKey(o.getSolicitudServicioId())) {
                            solicitudService.findById(o.getSolicitudServicioId())
                                    .ifPresent(s -> solicitudesMap.put(s.getId(), s));
                        }
                    }
                }
            }
        }

        if (solicitudesMap.isEmpty()) {
            return Optional.empty();
        }

        for (SolicitudServicio existente : solicitudesMap.values()) {
            if (existente.getId() != null && existente.getId().equals(solicitudObjetivo.getId())) {
                continue;
            }
            String st = existente.getEstadoSolicitud() != null ? existente.getEstadoSolicitud().toUpperCase() : "";
            if ("FINALIZADA".equals(st) || "COMPLETADO".equals(st) || st.startsWith("CANCELADA") || st.startsWith("CANCELADO")) {
                continue;
            }

            Oferta ofertaAceptadaExistente = ofertasAceptadasMap.get(existente.getId());
            if (ofertaAceptadaExistente == null && ofertaService != null) {
                ofertaAceptadaExistente = ofertaService.findBySolicitudId(existente.getId()).stream()
                        .filter(o -> proveedorId.equals(o.getPerfilProveedorId()) && "ACEPTADA".equalsIgnoreCase(o.getEstadoOferta()))
                        .findFirst()
                        .orElse(null);
            }

            List<IntervaloTiempo> intervalosExistentes = calcularIntervalosOferta(existente, ofertaAceptadaExistente);
            for (IntervaloTiempo nuevo : intervalosNuevos) {
                for (IntervaloTiempo act : intervalosExistentes) {
                    if (nuevo.inicio().isBefore(act.fin()) && nuevo.fin().isAfter(act.inicio())) {
                        return Optional.of(existente);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private record DuracionResuelta(
            UnidadDuracionOferta unidad,
            BigDecimal cantidad,
            boolean mismoHorarioDias,
            List<OfertaCronogramaDia> cronogramaDias
    ) {}

    private DuracionResuelta procesarYValidarDuracion(AproximacionDuracionRequest req) {
        if (req == null) {
            return new DuracionResuelta(UnidadDuracionOferta.HORAS, BigDecimal.ONE, false, List.of());
        }

        UnidadDuracionOferta unidad = req.unidad() != null ? req.unidad() : UnidadDuracionOferta.HORAS;
        boolean mismoHorario = Boolean.TRUE.equals(req.mismoHorarioTodosLosDias());

        if (unidad == UnidadDuracionOferta.HORAS || unidad == UnidadDuracionOferta.MINUTOS) {
            BigDecimal cantidad = req.cantidad();
            if (cantidad == null) {
                if (unidad == UnidadDuracionOferta.HORAS) {
                    cantidad = BigDecimal.ONE;
                } else {
                    throw new IllegalArgumentException("Cuando la duración es en MINUTOS, debe indicar una cantidad entera mayor a 30.");
                }
            }
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad de duración aproximada debe ser mayor a cero.");
            }
            if (unidad == UnidadDuracionOferta.MINUTOS) {
                if (cantidad.stripTrailingZeros().scale() > 0) {
                    throw new IllegalArgumentException("Cuando la duración es en MINUTOS, la cantidad debe ser un número entero.");
                }
                if (cantidad.compareTo(BigDecimal.valueOf(30)) < 0) {
                    throw new IllegalArgumentException("Cuando la duración es en MINUTOS, la cantidad debe ser de al menos 30 minutos.");
                }
            }
            return new DuracionResuelta(unidad, cantidad, false, List.of());
        }

        // Unidad == DIAS
        List<DiaTrabajoOfertaRequest> diasEntrada = new ArrayList<>();
        if (req.cronogramaDias() != null && !req.cronogramaDias().isEmpty()) {
            diasEntrada.addAll(req.cronogramaDias());
        } else if (req.fechaInicio() != null && req.fechaFin() != null) {
            if (req.fechaFin().isBefore(req.fechaInicio())) {
                throw new IllegalArgumentException("La fecha de fin (" + req.fechaFin() + ") no puede ser anterior a la fecha de inicio (" + req.fechaInicio() + ").");
            }
            Set<String> filtroDiasSemana = normalizarDiasSemana(req.diasSemanaSeleccionados());
            LocalDate cursor = req.fechaInicio();
            while (!cursor.isAfter(req.fechaFin())) {
                String nombreDia = obtenerNombreDiaEspanol(cursor.getDayOfWeek());
                if (filtroDiasSemana.isEmpty() || filtroDiasSemana.contains(nombreDia)) {
                    diasEntrada.add(new DiaTrabajoOfertaRequest(
                            cursor,
                            nombreDia,
                            req.horaInicioGeneral(),
                            req.horaFinGeneral(),
                            req.horasTrabajoPorDia()
                    ));
                }
                cursor = cursor.plusDays(1);
            }
        }

        if (diasEntrada.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cuando la duración es por DIAS, debe indicar cuáles días trabajará y las horas de trabajo en cada día (en 'cronogramaDias' o mediante 'fechaInicio' y 'fechaFin')."
            );
        }

        Set<LocalDate> fechasUnicas = new LinkedHashSet<>();
        List<OfertaCronogramaDia> cronogramaResuelto = new ArrayList<>();

        for (DiaTrabajoOfertaRequest diaReq : diasEntrada) {
            if (diaReq == null || diaReq.fecha() == null) {
                throw new IllegalArgumentException("Cada día del cronograma debe tener una fecha válida (YYYY-MM-DD).");
            }

            LocalDate fecha = diaReq.fecha();
            fechasUnicas.add(fecha);
            String diaSemana = obtenerNombreDiaEspanol(fecha.getDayOfWeek());

            if (diaReq.franjas() != null && !diaReq.franjas().isEmpty()) {
                for (FranjaHorariaOfertaRequest franja : diaReq.franjas()) {
                    if (franja == null) continue;
                    cronogramaResuelto.add(construirBloqueHorarioDia(
                            fecha,
                            diaSemana,
                            franja.horaInicio(),
                            franja.horaFin(),
                            franja.horasTrabajo()
                    ));
                }
            } else {
                LocalTime horaInicio = diaReq.horaInicio() != null
                        ? diaReq.horaInicio()
                        : (mismoHorario ? req.horaInicioGeneral() : null);

                LocalTime horaFin = diaReq.horaFin() != null
                        ? diaReq.horaFin()
                        : (mismoHorario ? req.horaFinGeneral() : null);

                BigDecimal horasTrabajo = diaReq.horasTrabajo() != null
                        ? diaReq.horasTrabajo()
                        : (mismoHorario ? req.horasTrabajoPorDia() : null);

                cronogramaResuelto.add(construirBloqueHorarioDia(
                        fecha,
                        diaSemana,
                        horaInicio,
                        horaFin,
                        horasTrabajo
                ));
            }
        }

        // Validar que la suma de horas por cada fecha individual no supere 24h
        Map<LocalDate, BigDecimal> horasPorFecha = cronogramaResuelto.stream()
                .collect(Collectors.groupingBy(
                        OfertaCronogramaDia::getFecha,
                        Collectors.reducing(BigDecimal.ZERO, OfertaCronogramaDia::getHorasTrabajo, BigDecimal::add)
                ));
        for (Map.Entry<LocalDate, BigDecimal> entry : horasPorFecha.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.valueOf(24)) > 0) {
                throw new IllegalArgumentException(
                        "Las horas de trabajo acumuladas en el día " + entry.getKey() + " (" + entry.getValue() + "h) no pueden superar las 24 horas."
                );
            }
        }

        cronogramaResuelto.sort(
                Comparator.comparing(OfertaCronogramaDia::getFecha)
                        .thenComparing(OfertaCronogramaDia::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        int totalDiasDistintos = fechasUnicas.size();
        BigDecimal cantidadDias = BigDecimal.valueOf(totalDiasDistintos);
        if (req.cantidad() != null) {
            if (req.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad de días debe ser mayor a cero.");
            }
            if (req.cantidad().stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("Cuando la unidad es DIAS, la cantidad de días debe ser un número entero.");
            }
            if (req.cantidad().intValue() != totalDiasDistintos) {
                throw new IllegalArgumentException(
                        "La cantidad de días indicada (" + req.cantidad().intValue() + ") no coincide con los días configurados en el cronograma (" + totalDiasDistintos + ")."
                );
            }
            cantidadDias = req.cantidad();
        }

        return new DuracionResuelta(UnidadDuracionOferta.DIAS, cantidadDias, mismoHorario, cronogramaResuelto);
    }

    private OfertaCronogramaDia construirBloqueHorarioDia(
            LocalDate fecha,
            String diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            BigDecimal horasTrabajo
    ) {
        if (horaInicio != null && horaFin != null) {
            if (!horaFin.isAfter(horaInicio)) {
                throw new IllegalArgumentException(
                        "En el día " + fecha + " (" + diaSemana + "), la hora de fin (" + horaFin + ") debe ser posterior a la hora de inicio (" + horaInicio + ")."
                );
            }
            long minutos = Duration.between(horaInicio, horaFin).toMinutes();
            BigDecimal horasCalculadas = BigDecimal.valueOf(minutos)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            if (horasTrabajo == null) {
                horasTrabajo = horasCalculadas;
            }
        } else if (horaInicio != null && horasTrabajo != null && horasTrabajo.compareTo(BigDecimal.ZERO) > 0) {
            long minutosASumar = horasTrabajo.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).longValue();
            horaFin = horaInicio.plusMinutes(minutosASumar);
        }

        if (horasTrabajo == null || horasTrabajo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Debe indicar las horas a trabajar (o rango de horaInicio y horaFin) para el día " + fecha + " (" + diaSemana + ")."
            );
        }
        if (horasTrabajo.compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new IllegalArgumentException(
                    "Las horas de trabajo en un solo día (" + fecha + ") no pueden superar las 24 horas."
            );
        }

        return OfertaCronogramaDia.builder()
                .fecha(fecha)
                .diaSemana(diaSemana)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .horasTrabajo(horasTrabajo)
                .build();
    }

    private Set<String> normalizarDiasSemana(Set<String> dias) {
        if (dias == null || dias.isEmpty()) {
            return Set.of();
        }
        return dias.stream()
                .filter(Objects::nonNull)
                .map(d -> Normalizer.normalize(d.trim(), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private String obtenerNombreDiaEspanol(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }

    private String generarResumenTiempoEstimado(DuracionResuelta d) {
        return switch (d.unidad()) {
            case MINUTOS -> d.cantidad().stripTrailingZeros().toPlainString() + " min";
            case HORAS -> {
                String cant = d.cantidad().stripTrailingZeros().toPlainString();
                yield "1".equals(cant) ? "1 hora" : cant + " horas";
            }
            case DIAS -> {
                BigDecimal totalHoras = d.cronogramaDias().stream()
                        .map(OfertaCronogramaDia::getHorasTrabajo)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                long dias = d.cronogramaDias().stream()
                        .map(OfertaCronogramaDia::getFecha)
                        .distinct()
                        .count();
                yield dias + (dias == 1 ? " día" : " días") + " (" + totalHoras.stripTrailingZeros().toPlainString() + "h totales)";
            }
        };
    }

    @Override
    public List<OfertaResponse> listarOfertasPorSolicitud(Long solicitudId) {
        List<Oferta> ofertas = ofertaService.findBySolicitudId(solicitudId);
        List<OfertaResponse> resultado = new ArrayList<>();
        for (Oferta o : ofertas) {
            if ("ENVIADA".equalsIgnoreCase(o.getEstadoOferta()) && haExpiradoOferta(o)) {
                o.setEstadoOferta("EXPIRADA");
                ofertaService.save(o);
                continue;
            }
            // Del lado del cliente solo son visibles ofertas vigentes (ENVIADA o ACEPTADA),
            // ocultando RETIRADA, EXPIRADA (>10 min) y PENDIENTE_REOFERTA (cuando el cliente editó la solicitud y espera reoferta)
            if ("RETIRADA".equalsIgnoreCase(o.getEstadoOferta())
                    || "EXPIRADA".equalsIgnoreCase(o.getEstadoOferta())
                    || "PENDIENTE_REOFERTA".equalsIgnoreCase(o.getEstadoOferta())) {
                continue;
            }
            resultado.add(toResponse(o));
        }
        return resultado;
    }

    @Override
    public OfertaResponse aceptarOferta(Long ofertaId) {
        Oferta oferta = ofertaService.findById(ofertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada con id: " + ofertaId));

        if ("ENVIADA".equalsIgnoreCase(oferta.getEstadoOferta()) && haExpiradoOferta(oferta)) {
            oferta.setEstadoOferta("EXPIRADA");
            ofertaService.save(oferta);
            throw new IllegalStateException("La oferta ha expirado tras superar el tiempo límite de 10 minutos.");
        }

        if ("PENDIENTE_REOFERTA".equalsIgnoreCase(oferta.getEstadoOferta())) {
            throw new IllegalStateException("La solicitud fue modificada por el cliente; el proveedor debe confirmar y reenviar su oferta antes de aceptarla.");
        }

        if (!"ENVIADA".equals(oferta.getEstadoOferta())) {
            throw new IllegalStateException("Solo se pueden aceptar ofertas en estado ENVIADA.");
        }

        SolicitudServicio solicitud = solicitudService.findById(oferta.getSolicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        // Seguro final de no solapamiento antes de aceptar (por si otra solicitud y oferta se aceptó al mismo tiempo)
        validarNoSolapamientoHorarioProveedor(oferta.getPerfilProveedorId(), solicitud, oferta);

        String estadoAnterior = solicitud.getEstadoSolicitud();

        // Delegate state transition via State Pattern
        solicitud.aceptarOferta(oferta);

        // Generar y encriptar el PIN de 4 dígitos para iniciar el servicio (1234 en dev/local, aleatorio encriptado en qa/pdn)
        PinSeguridadHelper helper = pinSeguridadHelper != null ? pinSeguridadHelper : new PinSeguridadHelper();
        String pinPlano = helper.generarPinPlano();
        solicitud.setPinInicioEncrypted(helper.encriptarPin(pinPlano));
        solicitud.setRecordatorio1hEnviado(false);

        // Accept this offer
        oferta.setEstadoOferta("ACEPTADA");
        ofertaService.save(oferta);

        // Reject all other offers for the same solicitud
        List<Oferta> otrasOfertas = ofertaService.findBySolicitudId(oferta.getSolicitudServicioId()).stream()
                .filter(o -> !o.getId().equals(ofertaId))
                .peek(o -> o.setEstadoOferta("RECHAZADA"))
                .toList();
        if (!otrasOfertas.isEmpty()) {
            ofertaService.saveAll(otrasOfertas);
        }

        solicitudService.save(solicitud);

        if (auditoriaService != null) {
            auditoriaService.registrarCambioEstado(
                    solicitud.getId(),
                    estadoAnterior,
                    solicitud.getEstadoSolicitud(),
                    "CLIENTE",
                    solicitud.getPerfilClienteId(),
                    null,
                    null,
                    null,
                    "Oferta #" + oferta.getId() + " aceptada por el cliente"
            );
        }

        // Notify observers (Winning provider gets notification, all other eligible/offering providers get SOLICITUD_CERRADA dissipation event)
        PerfilProveedor proveedorAceptado = proveedorService.findById(oferta.getPerfilProveedorId()).orElse(null);
        Long municipioId = null;
        if (solicitud.getDireccionId() != null) {
            municipioId = direccionService.findById(solicitud.getDireccionId())
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }
        List<PerfilProveedor> proveedoresNotificados = proveedorService.findEligibleProviders(solicitud.getServicioId(), municipioId);
        eventPublisher.notifyOfertaAceptada(solicitud, oferta, otrasOfertas, proveedorAceptado, proveedoresNotificados);

        return toResponse(oferta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> obtenerSolicitudesDisponibles(Long proveedorId) {
        proveedorService.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + proveedorId));

        // Si el proveedor tiene una restricción disciplinaria vigente, no puede recibir ni ver nuevas solicitudes
        if (cancelacionesUseCase != null && cancelacionesUseCase.tieneRestriccionVigente(proveedorId)) {
            return List.of();
        }

        // 1. Obtener especialidades activas del proveedor
        List<PerfilEspecialista> especialidades = perfilEspecialistaService.findByProveedorId(proveedorId).stream()
                .filter(PerfilEspecialista::isActive)
                .toList();

        if (especialidades.isEmpty()) {
            return List.of();
        }

        Set<Long> serviciosHabilitados = especialidades.stream()
                .map(PerfilEspecialista::getServicioId)
                .collect(Collectors.toSet());

        // 2. Obtener cobertura del proveedor (si no tiene cobertura configurada, aplica cobertura total/nacional)
        List<ProveedorCobertura> coberturas = proveedorCoberturaService.findByProveedorId(proveedorId);
        Set<Long> municipiosCobertura = coberturas.stream()
                .map(ProveedorCobertura::getMunicipioId)
                .collect(Collectors.toSet());
        boolean coberturaTotal = municipiosCobertura.isEmpty();

        // 3. Filtrar solicitudes ABIERTAS o EN_NEGOCIACION que correspondan a su especialidad y cobertura,
        //    donde el proveedor no haya ofertado aún (o donde el cliente haya modificado la solicitud tras expirar la oferta de 10 min)
        List<SolicitudServicio> candidatas = new ArrayList<>();
        candidatas.addAll(solicitudService.findByEstado("ABIERTA"));
        candidatas.addAll(solicitudService.findByEstado("EN_NEGOCIACION"));

        return candidatas.stream()
                .filter(s -> s.getServicioId() != null && serviciosHabilitados.contains(s.getServicioId()))
                .filter(s -> {
                    if (coberturaTotal) return true;
                    if (s.getDireccionId() == null) return true;
                    return direccionService.findById(s.getDireccionId())
                            .map(d -> d.getMunicipioId() == null || municipiosCobertura.contains(d.getMunicipioId()))
                            .orElse(true);
                })
                .filter(s -> !ofertaService.existsBySolicitudIdAndProveedorId(s.getId(), proveedorId))
                .map(this::mapToDisponibleResponse)
                .toList();
    }

    @Override
    public List<OfertaProveedorDetalleResponse> obtenerMisOfertasProveedor(Long proveedorId) {
        proveedorService.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + proveedorId));

        List<Oferta> ofertasProveedor = ofertaService.findByProveedorId(proveedorId);
        if (ofertasProveedor == null || ofertasProveedor.isEmpty()) {
            return List.of();
        }

        List<OfertaProveedorDetalleResponse> resultado = new ArrayList<>();

        for (Oferta o : ofertasProveedor) {
            // Si estaba ENVIADA y ya pasaron los 10 minutos, expira y desaparece de la pestaña
            if ("ENVIADA".equalsIgnoreCase(o.getEstadoOferta()) && haExpiradoOferta(o)) {
                procesarExpiracionOferta(o);
                continue;
            }

            // En la pestaña "Ofertas" solo aparecen ofertas activas (ENVIADA) o que esperan reoferta por edición del cliente (PENDIENTE_REOFERTA)
            if (!"ENVIADA".equalsIgnoreCase(o.getEstadoOferta())
                    && !"PENDIENTE_REOFERTA".equalsIgnoreCase(o.getEstadoOferta())) {
                continue;
            }

            SolicitudServicio solicitud = solicitudService.findById(o.getSolicitudServicioId()).orElse(null);
            if (solicitud == null) continue;

            // Si la solicitud ya fue cancelada o aceptada para otro proveedor, no mostrarla aquí
            String estSol = solicitud.getEstadoSolicitud() != null ? solicitud.getEstadoSolicitud().toUpperCase() : "";
            if (!"ABIERTA".equals(estSol) && !"EN_NEGOCIACION".equals(estSol)) {
                continue;
            }

            // Verificar en vivo si existe solapamiento horario con otro trabajo aceptado del proveedor
            boolean solapa = Boolean.TRUE.equals(o.getSolapamientoHorario())
                    || verificarSolapamientoHorario(proveedorId, solicitud, o);
            if (solapa != Boolean.TRUE.equals(o.getSolapamientoHorario())) {
                o.setSolapamientoHorario(solapa);
                ofertaService.save(o);
            }

            OfertaResponse ofertaResp = toResponse(o);
            SolicitudResponse solicitudResp = mapToDisponibleResponse(solicitud);

            resultado.add(new OfertaProveedorDetalleResponse(
                    o.getId(),
                    solicitud.getId(),
                    ofertaResp,
                    solicitudResp,
                    ofertaResp.solicitudModificada(),
                    ofertaResp.camposModificados(),
                    ofertaResp.valoresAnteriores(),
                    ofertaResp.solapamientoHorario(),
                    ofertaResp.ofertadoAt(),
                    ofertaResp.expiresAt(),
                    ofertaResp.remainingSeconds()
            ));
        }

        return resultado;
    }

    private SolicitudResponse mapToDisponibleResponse(SolicitudServicio s) {
        String servicioNombre = null;
        String categoriaNombre = null;
        if (s.getServicioId() != null) {
            Servicio serv = servicioPort.findById(s.getServicioId()).orElse(null);
            if (serv != null) {
                servicioNombre = serv.getNombre();
                if (serv.getCategoriaId() != null) {
                    categoriaNombre = categoriaPort.findById(serv.getCategoriaId())
                            .map(Categoria::getNombre)
                            .orElse(null);
                }
            }
        }

        String direccionTexto = null;
        String municipioNombre = null;
        String codigoDane = null;
        if (s.getDireccionId() != null) {
            Direccion dir = direccionService.findById(s.getDireccionId()).orElse(null);
            if (dir != null) {
                direccionTexto = dir.getDireccionTexto();
                if (dir.getMunicipioId() != null) {
                    Municipio mun = municipioPort.findById(dir.getMunicipioId()).orElse(null);
                    if (mun != null) {
                        municipioNombre = mun.getNombre();
                        codigoDane = mun.getCodigoDane();
                    }
                }
            }
        }

        String nombreContacto = s.getNombreContacto();
        BigDecimal clienteCalificacion = BigDecimal.valueOf(5.0);
        if (s.getPerfilClienteId() != null) {
            PerfilCliente cliente = perfilClienteService.findById(s.getPerfilClienteId()).orElse(null);
            if (cliente != null) {
                if (nombreContacto == null || nombreContacto.isBlank()) {
                    nombreContacto = cliente.getNombreCompleto();
                }
                if (cliente.getCalificacionPromedio() != null) {
                    clienteCalificacion = cliente.getCalificacionPromedio();
                }
            }
        }

        return new SolicitudResponse(
                s.getId(),
                servicioNombre,
                categoriaNombre,
                direccionTexto,
                municipioNombre,
                codigoDane,
                2.5, // Distancia estimada de proximidad en km
                s.getFechaProgramada(),
                nombreContacto,
                null, // Teléfono oculto por privacidad en solicitudes abiertas
                clienteCalificacion,
                s.getPrioridad(),
                s.getPresupuestoMaximo(),
                s.getEsPresupuestoEstricto(),
                null, // tarifaAcordada aún no definida
                s.getDetalles(),
                s.getFotos(),
                s.getEstadoSolicitud(),
                null, // calificacionRecibida
                null  // resenaCliente
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> obtenerMisSolicitudes(Long proveedorId, String estado) {
        proveedorService.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + proveedorId));

        String estadoFiltro = (estado != null && !estado.isBlank()) ? estado.toUpperCase() : null;

        return solicitudService.findByProveedorIdViaOfertaAceptada(proveedorId, estadoFiltro).stream()
                .map(s -> {
                    String servicioNombre = null;
                    String categoriaNombre = null;
                    if (s.getServicioId() != null) {
                        Servicio serv = servicioPort.findById(s.getServicioId()).orElse(null);
                        if (serv != null) {
                            servicioNombre = serv.getNombre();
                            if (serv.getCategoriaId() != null) {
                                categoriaNombre = categoriaPort.findById(serv.getCategoriaId())
                                        .map(Categoria::getNombre)
                                        .orElse(null);
                            }
                        }
                    }

                    String direccionTexto = null;
                    String municipioNombre = null;
                    String codigoDane = null;
                    if (s.getDireccionId() != null) {
                        Direccion dir = direccionService.findById(s.getDireccionId()).orElse(null);
                        if (dir != null) {
                            direccionTexto = dir.getDireccionTexto();
                            if (dir.getMunicipioId() != null) {
                                Municipio mun = municipioPort.findById(dir.getMunicipioId()).orElse(null);
                                if (mun != null) {
                                    municipioNombre = mun.getNombre();
                                    codigoDane = mun.getCodigoDane();
                                }
                            }
                        }
                    }

                    BigDecimal tarifaAcordada = ofertaService.findBySolicitudId(s.getId()).stream()
                            .filter(o -> proveedorId.equals(o.getPerfilProveedorId()) && "ACEPTADA".equals(o.getEstadoOferta()))
                            .map(Oferta::getValorPropuesto)
                            .findFirst()
                            .orElse(s.getPresupuestoMaximo());

                    BigDecimal calificacionRecibida = null;
                    String resenaCliente = null;
                    List<Calificacion> calificaciones = calificacionService.findByEvaluadoId(proveedorId);
                    for (Calificacion c : calificaciones) {
                        if (s.getId().equals(c.getSolicitudServicioId())) {
                            calificacionRecibida = c.getPuntaje() != null ? BigDecimal.valueOf(c.getPuntaje()) : null;
                            resenaCliente = c.getComentario();
                            break;
                        }
                    }

                    String nombreContacto = s.getNombreContacto();
                    String telefonoContacto = s.getTelefonoContacto();
                    BigDecimal clienteCalificacion = BigDecimal.valueOf(5.0);
                    if (s.getPerfilClienteId() != null) {
                        PerfilCliente cliente = perfilClienteService.findById(s.getPerfilClienteId()).orElse(null);
                        if (cliente != null) {
                            if (nombreContacto == null || nombreContacto.isBlank()) {
                                nombreContacto = cliente.getNombreCompleto();
                            }
                            if (telefonoContacto == null || telefonoContacto.isBlank()) {
                                telefonoContacto = cliente.getCelular();
                            }
                            if (cliente.getCalificacionPromedio() != null) {
                                clienteCalificacion = cliente.getCalificacionPromedio();
                            }
                        }
                    }

                    return new SolicitudResponse(
                            s.getId(),
                            servicioNombre,
                            categoriaNombre,
                            direccionTexto,
                            municipioNombre,
                            codigoDane,
                            0.0,
                            s.getFechaProgramada(),
                            nombreContacto,
                            telefonoContacto,
                            clienteCalificacion,
                            s.getPrioridad(),
                            s.getPresupuestoMaximo(),
                            s.getEsPresupuestoEstricto(),
                            tarifaAcordada,
                            s.getDetalles(),
                            s.getFotos(),
                            s.getEstadoSolicitud(),
                            calificacionRecibida,
                            resenaCliente
                    );
                })
                .toList();
    }

    private OfertaResponse toResponse(Oferta o) {
        PerfilProveedor proveedor = proveedorService.findById(o.getPerfilProveedorId()).orElse(null);
        String nombreProveedor = proveedor != null ? proveedor.getNombreCompleto() : "Proveedor FINDU";
        String fotoProveedor = proveedor != null ? fotoUrlResolver.resolve(proveedor) : null;
        var calificacion = proveedor != null ? proveedor.getCalificacionPromedio() : null;

        UnidadDuracionOferta unidad = o.getDuracionUnidad() != null ? o.getDuracionUnidad() : UnidadDuracionOferta.HORAS;
        BigDecimal cantidad = o.getDuracionCantidad() != null ? o.getDuracionCantidad() : BigDecimal.ONE;
        Boolean mismoHorario = Boolean.TRUE.equals(o.getMismoHorarioDias());

        List<DiaTrabajoOfertaResponse> diasResponse = o.getCronogramaDias() != null
                ? o.getCronogramaDias().stream()
                    .map(d -> new DiaTrabajoOfertaResponse(
                            d.getId(),
                            d.getFecha(),
                            d.getDiaSemana(),
                            d.getHoraInicio(),
                            d.getHoraFin(),
                            d.getHorasTrabajo()
                    ))
                    .toList()
                : List.of();

        BigDecimal totalHorasEstimadas = switch (unidad) {
            case HORAS -> cantidad;
            case MINUTOS -> cantidad.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            case DIAS -> diasResponse.stream()
                    .map(DiaTrabajoOfertaResponse::horasTrabajo)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        };

        AproximacionDuracionResponse duracionResponse = new AproximacionDuracionResponse(
                unidad,
                cantidad,
                mismoHorario,
                totalHorasEstimadas,
                diasResponse
        );

        LocalDateTime ofertadoAt = o.getOfertadoAt() != null
                ? o.getOfertadoAt()
                : (o.getUpdatedAt() != null ? o.getUpdatedAt() : o.getCreatedAt());
        LocalDateTime expiresAt = ofertadoAt != null ? ofertadoAt.plusMinutes(EXPIRACION_OFERTA_MINUTOS) : null;
        Long remainingSeconds = expiresAt != null
                ? Math.max(0L, Duration.between(LocalDateTime.now(), expiresAt).getSeconds())
                : null;

        List<String> listaCamposModificados = o.getCamposModificados() != null && !o.getCamposModificados().isBlank()
                ? Arrays.stream(o.getCamposModificados().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
                : List.of();

        Map<String, String> mapaValoresAnteriores = Map.of();
        if (o.getValoresAnteriores() != null && !o.getValoresAnteriores().isBlank()) {
            try {
                mapaValoresAnteriores = JSON_MAPPER.readValue(o.getValoresAnteriores(), new TypeReference<Map<String, String>>() {});
            } catch (Exception ignored) {
            }
        }

        return new OfertaResponse(
                o.getId(),
                o.getPerfilProveedorId(),
                nombreProveedor,
                fotoProveedor,
                calificacion,
                o.getValorPropuesto(),
                o.getHoraInicioPropuesta(),
                o.getTiempoEstimado(),
                o.getMensajePresentacion(),
                o.getEstadoOferta(),
                duracionResponse,
                ofertadoAt,
                expiresAt,
                remainingSeconds,
                Boolean.TRUE.equals(o.getSolicitudModificada()),
                listaCamposModificados,
                mapaValoresAnteriores,
                Boolean.TRUE.equals(o.getSolapamientoHorario())
        );
    }
}
