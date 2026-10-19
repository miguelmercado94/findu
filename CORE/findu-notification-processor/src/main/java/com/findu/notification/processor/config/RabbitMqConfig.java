package com.findu.notification.processor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${findu.notification.exchange}")
    private String exchange;

    @Value("${findu.notification.queue.push}")
    private String pushQueue;

    @Value("${findu.notification.queue.email}")
    private String emailQueue;

    @Value("${findu.notification.queue.sms}")
    private String smsQueue;

    @Value("${findu.notification.queue.whatsapp:findu.notifications.whatsapp}")
    private String whatsappQueue;

    @Value("${findu.notification.queue.alerts:findu.notifications.alerts}")
    private String alertsQueue;

    @Value("${findu.rabbitmq.exchanges.events:findu.events.exchange}")
    private String eventsExchange;

    @Value("${findu.rabbitmq.queues.fallback:findu.notifications.fallback.queue}")
    private String fallbackQueue;

    @Value("${findu.rabbitmq.exchanges.fallback:findu.notifications.fallback.exchange}")
    private String fallbackExchange;

    @Value("${findu.rabbitmq.routing-keys.fallback:findu.fallback.notification}")
    private String fallbackRoutingKey;

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(eventsExchange, true, false);
    }

    @Bean
    public Queue pushQueue() {
        return QueueBuilder.durable(pushQueue).build();
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(emailQueue).build();
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(smsQueue).build();
    }

    @Bean
    public Queue whatsappQueue() {
        return QueueBuilder.durable(whatsappQueue).build();
    }

    @Bean
    public Queue alertsQueue() {
        return QueueBuilder.durable(alertsQueue).build();
    }

    @Bean
    public Binding pushBinding() {
        return BindingBuilder.bind(pushQueue()).to(notificationExchange()).with("notification.push");
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(notificationExchange()).with("notification.correo");
    }

    @Bean
    public Binding emailBindingAlt() {
        return BindingBuilder.bind(emailQueue()).to(notificationExchange()).with("notification.email");
    }

    @Bean
    public Binding smsBinding() {
        return BindingBuilder.bind(smsQueue()).to(notificationExchange()).with("notification.celular");
    }

    @Bean
    public Binding smsBindingAlt() {
        return BindingBuilder.bind(smsQueue()).to(notificationExchange()).with("notification.sms");
    }

    @Bean
    public Binding whatsappBinding() {
        return BindingBuilder.bind(whatsappQueue()).to(notificationExchange()).with("notification.whatsapp");
    }

    @Bean
    public Binding alertPendingBinding() {
        return BindingBuilder.bind(alertsQueue()).to(notificationExchange()).with("alert.pending");
    }

    @Bean
    public Binding alertResolvedBinding() {
        return BindingBuilder.bind(alertsQueue()).to(notificationExchange()).with("alert.resolved");
    }

    @Bean
    public DirectExchange fallbackExchange() {
        return new DirectExchange(fallbackExchange, true, false);
    }

    @Bean
    public Queue fallbackQueue() {
        return QueueBuilder.durable(fallbackQueue).build();
    }

    @Bean
    public Binding fallbackBinding() {
        return BindingBuilder.bind(fallbackQueue()).to(fallbackExchange()).with(fallbackRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper mapper = new DefaultJackson2JavaTypeMapper() {
            @Override
            protected String retrieveHeaderAsString(MessageProperties properties, String headerName) {
                return null;
            }
        };
        mapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        mapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(mapper);
        converter.setClassMapper(null);
        return converter;
    }
}
