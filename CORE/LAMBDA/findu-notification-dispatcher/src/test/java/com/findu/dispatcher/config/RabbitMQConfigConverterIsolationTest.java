package com.findu.dispatcher.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el converter JSON de la ruta transaccional NO se filtra al cableado AMQP por
 * defecto (RabbitTemplate del RabbitFallbackProducer y SimpleRabbitListenerContainerFactory del
 * RabbitEventConsumer), de modo que el flujo de presencia WebSocket (texto plano) queda intacto.
 *
 * El converter se construye como variable local dentro de notificationsRabbitTemplate, por lo que
 * el contexto NO debe contener ningún bean de tipo MessageConverter; de haberlo (único), Spring
 * Boot lo adoptaría por defecto (getIfUnique) y rompería la presencia.
 */
class RabbitMQConfigConverterIsolationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ConnectionFactory.class, () -> Mockito.mock(ConnectionFactory.class))
            .withUserConfiguration(RabbitMQConfig.class);

    @Test
    void noHayNingunMessageConverterComoBeanEnElContexto() {
        contextRunner.run(context -> {
            // Si existiera un único MessageConverter bean, Spring Boot lo inyectaría en el
            // RabbitTemplate y el listener factory por defecto, rompiendo la ruta de presencia.
            assertThat(context).doesNotHaveBean(MessageConverter.class);
            assertThat(context).doesNotHaveBean(Jackson2JsonMessageConverter.class);
        });
    }

    @Test
    void elRabbitTemplateTransaccionalUsaConverterJsonAislado() {
        contextRunner.run(context -> {
            RabbitTemplate transaccional = context.getBean("notificationsRabbitTemplate", RabbitTemplate.class);
            // El template transaccional sí lleva el converter Jackson (contentType=application/json
            // para que el processor deserialice a NotificationMessage), pero de forma aislada.
            assertThat(transaccional.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
        });
    }

    @Test
    void unRabbitTemplatePorDefectoConservaSimpleMessageConverter() {
        // Simula el comportamiento del template autoconfigurado por defecto (sin converter inyectado
        // desde el contexto): mantiene SimpleMessageConverter, que es lo que la ruta de presencia
        // (RabbitFallbackProducer con String ya serializado) necesita.
        RabbitTemplate porDefecto = new RabbitTemplate(Mockito.mock(ConnectionFactory.class));
        assertThat(porDefecto.getMessageConverter()).isInstanceOf(SimpleMessageConverter.class);
    }
}
