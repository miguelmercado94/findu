package com.findu.notification.processor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("FINDU Notification Processor — Motor de Plantillas")
                .version("1.0.0")
                .description("Motor de plantillas dinámico y gestionable por un rol admin. Soporta 6 tipos funcionales "
                        + "(SMS, EMAIL, WHATSAPP, EVENTO, NOTIFICACION, ALERTA) con almacenamiento versionado en DynamoDB "
                        + "(PK id=CODE#TIPO#LANG + SK v#<n>/v#ACTIVE, histórico y rollback) y endpoints de CRUD de admin. "
                        + "Despacha vía AWS SNS (SMS), AWS SES (email), WhatsApp Business, RabbitMQ (eventos entre micros) "
                        + "y WebSocket/FCM (notificaciones en tiempo real), además de entregar alertas de UI al front."));
    }
}
