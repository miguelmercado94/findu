package com.findu.core.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudServicio {
    private Long id;
    private Long perfilClienteId;
    private Long servicioId;
    private Long direccionId;
    private LocalDateTime fechaProgramada;
    private String nombreContacto;
    private String telefonoContacto;
    private Integer prioridad;
    private BigDecimal presupuestoMaximo;
    private Boolean esPresupuestoEstricto;
    private Integer cantidadEstimada;
    private String detalles;
    private String fotos;
    private String estadoSolicitud;
    private String pinInicioEncrypted;
    @Builder.Default
    private Boolean recordatorio1hEnviado = false;

    public com.findu.core.domain.model.state.SolicitudEstadoState getEstadoState() {
        if (estadoSolicitud == null) return new com.findu.core.domain.model.state.EstadoSolicitudAbierta();
        return switch (estadoSolicitud.toUpperCase()) {
            case "EN_NEGOCIACION" -> new com.findu.core.domain.model.state.EstadoSolicitudEnNegociacion();
            case "ACEPTADA", "PROGRAMADA", "CANCELACION_RECHAZADA" -> new com.findu.core.domain.model.state.EstadoSolicitudAceptada();
            case "EN_CURSO" -> new com.findu.core.domain.model.state.EstadoSolicitudEnCurso();
            case "FINALIZADA", "COMPLETADO" -> new com.findu.core.domain.model.state.EstadoSolicitudFinalizada();
            case "CANCELADA", "CANCELADO", "CANCELADA_SIN_PENALIDAD", "CANCELADA_CON_PENALIDAD" -> new com.findu.core.domain.model.state.EstadoSolicitudCancelada();
            default -> new com.findu.core.domain.model.state.EstadoSolicitudAbierta();
        };
    }

    public void recibirOferta(Oferta oferta) {
        getEstadoState().recibirOferta(this, oferta);
    }

    public void aceptarOferta(Oferta oferta) {
        getEstadoState().aceptarOferta(this, oferta);
    }

    public void iniciarServicio() {
        getEstadoState().iniciarServicio(this);
    }

    public void finalizarServicio() {
        getEstadoState().finalizarServicio(this);
    }

    public void cancelarSolicitud() {
        getEstadoState().cancelarSolicitud(this);
    }
}
