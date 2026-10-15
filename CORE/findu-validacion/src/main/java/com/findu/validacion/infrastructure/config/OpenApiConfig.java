package com.findu.validacion.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("FINDU Validación — Motor de Validaciones y Catálogo de Endpoints")
                .version("1.0.0")
                .description("Microservicio independiente reactivo para el catálogo declarativo de endpoints y reglas "
                        + "de validación (ACTIVE / PASSIVE), orquestación de procesos de negocio, emisión de alertas a RabbitMQ "
                        + "y resolución manual de soporte.")
                .contact(new Contact().name("FindU Team")));
    }
}
