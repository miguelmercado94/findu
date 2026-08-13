package com.findu.notification.processor.config;

import org.springframework.amqp.core.*;
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

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(exchange, true, false);
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
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
