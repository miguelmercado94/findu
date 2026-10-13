package com.findu.notification.processor.domain.enums;

/**
 * Tipo funcional de plantilla (diseño §1). Reemplaza el modelo "por canal":
 * un admin crea plantillas de estos 6 tipos sin desplegar código.
 */
public enum TemplateType {
    SMS,
    EMAIL,
    WHATSAPP,
    EVENTO,
    NOTIFICACION,
    ALERTA;

    /**
     * Mapea el canal técnico (usado por los senders / Template Method) al tipo
     * funcional de plantilla. El canal {@code PUSH} corresponde a {@code NOTIFICACION};
     * {@code EMAIL}/{@code SMS}/{@code WHATSAPP} son 1:1.
     */
    public static TemplateType fromChannel(NotificationChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("NotificationChannel es requerido");
        }
        return switch (channel) {
            case EMAIL -> EMAIL;
            case SMS -> SMS;
            case WHATSAPP -> WHATSAPP;
            case PUSH -> NOTIFICACION;
        };
    }
}
