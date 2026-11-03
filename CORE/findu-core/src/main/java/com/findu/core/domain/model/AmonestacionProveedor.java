package com.findu.core.domain.model;

import com.findu.core.domain.enums.NivelAmonestacion;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmonestacionProveedor {
    private Long id;
    private Long perfilProveedorId;
    private Long solicitudServicioId;
    private Long cancelacionId;
    private String ticketCodigo;
    private NivelAmonestacion nivel;
    private String motivo;
    private Integer conteo30Dias;
    private Integer diasRestriccion;
    private LocalDateTime restriccionInicio;
    private LocalDateTime restriccionFin;
    @Builder.Default
    private Boolean activa = true;
    private LocalDateTime createdAt;
}

