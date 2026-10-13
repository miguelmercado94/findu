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
    ALERTA
}
