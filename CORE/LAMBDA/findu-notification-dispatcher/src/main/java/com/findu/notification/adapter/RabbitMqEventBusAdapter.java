package com.findu.notification.adapter;

import com.findu.notification.model.NotificationRecord;
import com.findu.notification.port.EventBusPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementación de EventBusPort usando RabbitMQ (Amazon MQ).
 * Routing key: notification.{type} (ej: notification.email, notification.push, notification.sms)
 */
@Component
public class RabbitMqEventBusAdapter implements EventBusPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventBusAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String routingKeyPrefix;

    public RabbitMqEventBusAdapter(RabbitTemplate rabbitTemplate,
                                   @Value("${findu.notification.routing-key-prefix:notification.}") String routingKeyPrefix) {
        this.rabbitTemplate = rabbitTemplate;
        this.routingKeyPrefix = routingKeyPrefix;
    }

    @Override
    public void publish(NotificationRecord record) {
        String routingKey = routingKeyPrefix + record.getType().toLowerCase();
        log.info("Publishing to bus: routingKey={} notificationId={} recipient={}",
                routingKey, record.getNotificationId(), record.getRecipient());
        rabbitTemplate.convertAndSend(routingKey, record);
    }
}
