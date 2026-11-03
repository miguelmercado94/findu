package com.findu.core.domain.model;

import com.findu.core.domain.enums.UnidadDuracionOferta;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oferta {
    private Long id;
    private Long solicitudServicioId;
    private Long perfilProveedorId;
    private BigDecimal valorPropuesto;
    private LocalTime horaInicioPropuesta;
    private String tiempoEstimado;
    private String mensajePresentacion;
    private String estadoOferta;

    @Builder.Default
    private UnidadDuracionOferta duracionUnidad = UnidadDuracionOferta.HORAS;

    @Builder.Default
    private BigDecimal duracionCantidad = BigDecimal.ONE;

    @Builder.Default
    private Boolean mismoHorarioDias = false;

    @Builder.Default
    private List<OfertaCronogramaDia> cronogramaDias = new ArrayList<>();

    private LocalDateTime ofertadoAt;

    @Builder.Default
    private Boolean solicitudModificada = false;

    private String camposModificados;

    private String valoresAnteriores;

    @Builder.Default
    private Boolean solapamientoHorario = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
