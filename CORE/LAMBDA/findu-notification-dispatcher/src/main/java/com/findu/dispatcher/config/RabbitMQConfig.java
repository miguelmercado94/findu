package com.findu.dispatcher.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${findu.rabbitmq.exchanges.events:findu.events.exchange}")
    private String eventsExchangeName;

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
}
