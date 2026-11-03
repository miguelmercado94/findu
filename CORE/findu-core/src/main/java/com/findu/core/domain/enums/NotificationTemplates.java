package com.findu.core.domain.enums;

public final class NotificationTemplates {
    private NotificationTemplates() {}

    public static final String BIENVENIDA_CLIENTE = "BIENVENIDA_CLIENTE";
    public static final String BIENVENIDA_PROVEEDOR = "BIENVENIDA_PROVEEDOR";
    public static final String SOLICITUD_CREADA = "SOLICITUD_CREADA";
    public static final String OFERTA_RECIBIDA = "OFERTA_RECIBIDA";
    public static final String OFERTA_ACEPTADA = "OFERTA_ACEPTADA";
    public static final String SOLICITUD_CANCELADA = "SOLICITUD_CANCELADA";
    public static final String SOLICITUD_ACTUALIZADA = "SOLICITUD_ACTUALIZADA";
    public static final String CALIFICACION_RECIBIDA = "CALIFICACION_RECIBIDA";
    public static final String SERVICIO_PROGRAMADO = "SERVICIO_PROGRAMADO";

    // Alarma 1 hora antes del inicio del servicio
    public static final String ALARMA_SERVICIO_1H = "ALARMA_SERVICIO_1H";

    // Flujo de cancelaciones y amonestaciones - Proveedor
    public static final String SOLICITUD_CANCELACION_RECIBIDA = "SOLICITUD_CANCELACION_RECIBIDA";
    public static final String TICKET_CANCELACION_CREADO = "TICKET_CANCELACION_CREADO";
    public static final String SOPORTE_PENDIENTE_CARGAR = "SOPORTE_PENDIENTE_CARGAR";
    public static final String EVIDENCIA_CANCELACION_RECIBIDA = "EVIDENCIA_CANCELACION_RECIBIDA";
    public static final String EVIDENCIA_CANCELACION_RESUELTA = "EVIDENCIA_CANCELACION_RESUELTA";
    public static final String AMONESTACION_GENERADA = "AMONESTACION_GENERADA";
    public static final String RESTRICCION_ACTIVADA = "RESTRICCION_ACTIVADA";
    public static final String RESTRICCION_FINALIZADA = "RESTRICCION_FINALIZADA";
    public static final String RESOLUCION_SOLICITUD_CANCELACION = "RESOLUCION_SOLICITUD_CANCELACION";

    // Flujo de cancelaciones - Cliente
    public static final String SERVICIO_CANCELADO_POR_PROVEEDOR = "SERVICIO_CANCELADO_POR_PROVEEDOR";
    public static final String SERVICIO_DISPONIBLE_PARA_REABRIR = "SERVICIO_DISPONIBLE_PARA_REABRIR";
    public static final String CANCELACION_EJECUTADA_POR_SOPORTE = "CANCELACION_EJECUTADA_POR_SOPORTE";
}
