package com.findu.dispatcher.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${findu.rabbitmq.exchanges.events:findu.events.exchange}")
    private String eventsExchangeName;

    @Value("${findu.rabbitmq.notification.exchange:findu.notifications}")
    private String notificationsExchangeName;

    @Value("${findu.rabbitmq.exchanges.fallback:findu.notifications.fallback.exchange}")
    private String fallbackExchangeName;

    @Value("${findu.rabbitmq.queues.dispatcher:findu.dispatcher.events.queue}")
    private String dispatcherQueueName;

    @Value("${findu.rabbitmq.queues.fallback:findu.notifications.fallback.queue}")
    private String fallbackQueueName;

    @Value("${findu.rabbitmq.routing-keys.event:findu.event.#}")
    private String eventRoutingKey;

    @Value("${findu.rabbitmq.routing-keys.fallback:findu.fallback.notification}")
    private String fallbackRoutingKey;

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(eventsExchangeName, true, false);
    }

    @Bean
    public DirectExchange fallbackExchange() {
        return new DirectExchange(fallbackExchangeName, true, false);
    }

    @Bean
    public Queue dispatcherQueue() {
        return QueueBuilder.durable(dispatcherQueueName).build();
    }

    @Bean
    public Queue fallbackQueue() {
        return QueueBuilder.durable(fallbackQueueName).build();
    }

    @Bean
    public Binding dispatcherBinding(Queue dispatcherQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(dispatcherQueue).to(eventsExchange).with(eventRoutingKey);
    }

    @Bean
    public Binding fallbackBinding(Queue fallbackQueue, DirectExchange fallbackExchange) {
        return BindingBuilder.bind(fallbackQueue).to(fallbackExchange).with(fallbackRoutingKey);
    }

    // --- Ruta transaccional hacia el findu-notification-processor (exchange findu.notifications) ---

    /**
     * Exchange destino que consume el findu-notification-processor. Se declara también aquí
     * (durable, no auto-delete) para que exista aunque el processor no haya arrancado todavía.
     * No se declaran las colas del processor: el dispatcher solo publica al exchange.
     */
    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(notificationsExchangeName, true, false);
    }

    /**
     * RabbitTemplate cualificado para la ruta transaccional. Usa un Jackson2JsonMessageConverter
     * creado como variable LOCAL (no como bean) para que los mensajes transaccionales se publiquen
     * con contentType=application/json sin introducir un MessageConverter en el contexto.
     *
     * Importante: no se expone el converter como @Bean. Si fuese el único MessageConverter del
     * contexto, Spring Boot (getIfUnique) lo adoptaría en el RabbitTemplate por defecto que usa
     * RabbitFallbackProducer y en el SimpleRabbitListenerContainerFactory por defecto que alimenta
     * RabbitEventConsumer, alterando el contentType del fallback FCM y rompiendo el consumo String
     * del flujo de presencia WebSocket. Al mantenerlo local, el template por defecto y el listener
     * factory conservan su SimpleMessageConverter y la ruta de presencia queda intacta.
     */
    @Bean
    public RabbitTemplate notificationsRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
