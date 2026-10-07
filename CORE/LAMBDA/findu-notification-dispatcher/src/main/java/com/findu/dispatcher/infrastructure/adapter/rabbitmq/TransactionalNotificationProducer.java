package com.findu.dispatcher.infrastructure.adapter.rabbitmq;

import com.findu.dispatcher.presentation.controller.dto.NotificationMessagePayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Publica notificaciones transaccionales (SMS/EMAIL/PUSH/WHATSAPP) al exchange
 * {@code findu.notifications} con la routing key del canal, en un body JSON deserializable
 * por el NotificationMessage del findu-notification-processor.
 *
 * NO reutiliza el camino de fallback (RabbitFallbackProducer) de presencia WebSocket.
 */
@Slf4j
@Component
public class TransactionalNotificationProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String rkSms;
    private final String rkEmail;
    private final String rkPush;
    private final String rkWhatsapp;

    public TransactionalNotificationProducer(
            @Qualifier("notificationsRabbitTemplate") RabbitTemplate rabbitTemplate,
            @Value("${findu.rabbitmq.notification.exchange:findu.notifications}") String exchange,
            @Value("${findu.rabbitmq.notification.routing-keys.sms:notification.sms}") String rkSms,
            @Value("${findu.rabbitmq.notification.routing-keys.email:notification.email}") String rkEmail,
            @Value("${findu.rabbitmq.notification.routing-keys.push:notification.push}") String rkPush,
            @Value("${findu.rabbitmq.notification.routing-keys.whatsapp:notification.whatsapp}") String rkWhatsapp) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.rkSms = rkSms;
        this.rkEmail = rkEmail;
        this.rkPush = rkPush;
        this.rkWhatsapp = rkWhatsapp;
    }

    /**
     * Publica el payload transaccional al exchange {@code findu.notifications} usando la
     * routing key derivada del {@code type} del canal.
     */
    public Mono<Void> publish(NotificationMessagePayload payload) {
        return Mono.fromRunnable(() -> {
            String routingKey = resolveRoutingKey(payload.type());
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("Notificación transaccional publicada: exchange={}, routingKey={}, recipient={}, templateCode={}, type={}",
                    exchange, routingKey, payload.recipient(), payload.templateCode(), payload.type());
        });
    }

    private String resolveRoutingKey(String type) {
        String channel = type == null ? "" : type.trim().toUpperCase();
        return switch (channel) {
            case "SMS" -> rkSms;
            case "EMAIL" -> rkEmail;
            case "PUSH" -> rkPush;
            case "WHATSAPP" -> rkWhatsapp;
            default -> throw new IllegalArgumentException("Canal de notificación no soportado: " + type);
        };
    }
}
