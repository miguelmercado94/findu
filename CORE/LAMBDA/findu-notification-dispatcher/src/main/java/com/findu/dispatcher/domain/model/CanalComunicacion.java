package com.findu.dispatcher.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de Dominio que representa un Documento NoSQL de Canal de Comunicación (Mongo / DynamoDB)
 */
public class CanalComunicacion {
    private String id; // Identificador único (SolicitudId / TicketId)
    private TipoCanal tipoCanal; // CANAL_1_OFERTAS, CANAL_2_CHAT_PROVEEDOR, CANAL_3_SOPORTE
    private EstadoCanal estado; // ACTIVO, INACTIVO, CERRADO
    private List<Interoperante> interoperantes;
    private List<EventoCanal> eventos;
    private Instant fechaCreacion;
    private Instant fechaUltimaActividad;

    public CanalComunicacion() {
        this.interoperantes = new ArrayList<>();
        this.eventos = new ArrayList<>();
        this.fechaCreacion = Instant.now();
        this.fechaUltimaActividad = Instant.now();
        this.estado = EstadoCanal.ACTIVO;
    }

    public CanalComunicacion(String id, TipoCanal tipoCanal, List<Interoperante> interoperantes) {
        this();
        this.id = id;
        this.tipoCanal = tipoCanal;
        this.interoperantes = interoperantes != null ? interoperantes : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TipoCanal getTipoCanal() {
        return tipoCanal;
    }

    public void setTipoCanal(TipoCanal tipoCanal) {
        this.tipoCanal = tipoCanal;
    }

    public EstadoCanal getEstado() {
        return estado;
    }

    public void setEstado(EstadoCanal estado) {
        this.estado = estado;
    }

    public List<Interoperante> getInteroperantes() {
        return interoperantes;
    }

    public void setInteroperantes(List<Interoperante> interoperantes) {
        this.interoperantes = interoperantes;
    }

    public List<EventoCanal> getEventos() {
        return eventos;
    }

    public void setEventos(List<EventoCanal> eventos) {
        this.eventos = eventos;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaUltimaActividad() {
        return fechaUltimaActividad;
    }

    public void setFechaUltimaActividad(Instant fechaUltimaActividad) {
        this.fechaUltimaActividad = fechaUltimaActividad;
    }

    public void agregarEvento(EventoCanal evento) {
        if (this.eventos == null) {
            this.eventos = new ArrayList<>();
        }
        this.eventos.add(evento);
        this.fechaUltimaActividad = Instant.now();
    }
}
