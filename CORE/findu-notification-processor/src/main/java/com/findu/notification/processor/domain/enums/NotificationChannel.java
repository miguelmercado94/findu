package com.findu.notification.processor.domain.enums;

/**
 * Canal técnico de salida. Lo usan los senders (SNS/SES/FCM/WhatsApp) y la
 * Factory/Template Method. El tipo funcional de plantilla es {@link TemplateType}.
 */
public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH,
    WHATSAPP
}
