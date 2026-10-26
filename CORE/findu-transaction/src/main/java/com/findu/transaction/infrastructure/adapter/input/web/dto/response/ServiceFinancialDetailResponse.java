package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceFinancialDetailResponse {

    private ServiceIdentityDto identificacion;
    private EconomicSummaryDto resumenEconomico;
    private List<DiscountDto> descuentos;
    private List<ExtraChargeDto> cobrosAdicionales;
    private SettlementDetailDto liquidacion;
    private PaymentMethodDetailDto medioCobro;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceIdentityDto {
        private Long serviceRequestId;
        private String serviceCode;
        private String servicioNombre;
        private String especialidadNombre;
        private Instant fechaRealizacion;
        private String fechaRealizacionLabel;
        private String estadoServicio;
        private String estadoLiquidacion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EconomicSummaryDto {
        private BigDecimal totalCobradoCliente;
        private BigDecimal descuentoAplicado;
        private BigDecimal cobrosExtra;
        private BigDecimal totalPagadoCliente;
        private String moneda;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountDto {
        private String id;
        private String nombre;
        private String tipo;
        private BigDecimal valor;
        private String descripcion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtraChargeDto {
        private String id;
        private String concepto;
        private String descripcion;
        private BigDecimal importe;
        private String estadoAprobacion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementDetailDto {
        private BigDecimal baseServicio;
        private BigDecimal comisionFinduMonto;
        private BigDecimal comisionPorcentaje;
        private BigDecimal gananciaNetaProveedor;
        private String estado;
        private String estadoBadge;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodDetailDto {
        private String metodo;
        private String codigoMetodo;
        private String descripcion;
        private String estado;
        private String icono;
        private List<PaymentItemDto> transacciones;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentItemDto {
        private String id;
        private String referencia;
        private String metodo;
        private BigDecimal importe;
        private String estado;
        private Instant fecha;
    }
}
