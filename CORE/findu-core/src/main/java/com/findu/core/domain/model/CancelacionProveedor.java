package com.findu.core.domain.model;

import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelacionProveedor {
    private Long id;
    private Long solicitudServicioId;
    private Long ofertaId;
    private Long perfilProveedorId;
    private CategoriaCancelacionProveedor categoria;
    private String motivo;
    private String descripcion;
    private String evidenciasUrls;
    private String ticketCodigo;
    private String estadoCancelacion;
    private String estadoTicket;
    private Boolean esInjustificada;
    private LocalDateTime fechaLimiteEvidencia;
    private String decisionSoporte;
    private String justificacionSoporte;
    private Long agenteSoporteId;
    private LocalDateTime fechaResolucion;
    @Builder.Default
    private Boolean amonestacionGenerada = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

