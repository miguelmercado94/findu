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
                .title("FINDU Notification Processor")
                .version("1.0.0")
                .description("Microservicio que consume eventos de notificación desde Amazon MQ y los despacha via Firebase (push), AWS SES (email) y AWS SNS (SMS)."));
    }
}
