package com.findu.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import reactor.core.publisher.Mono;

/**
 * Habilita auditoría para entidades R2DBC.
 * Rellena created_at, updated_at, created_by, updated_by usando ReactiveAuditorAware.
 */
@Configuration
@EnableR2dbcAuditing
public class R2dbcAuditingConfig {

    /**
     * Proveedor del auditor actual (quién crea/modifica).
     * Retorna "system" cuando no hay usuario autenticado; luego puedes cambiarlo
     * para leer de ReactiveSecurityContextHolder.getContext().
     */
    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        return () -> Mono.just("system");
    }
}
