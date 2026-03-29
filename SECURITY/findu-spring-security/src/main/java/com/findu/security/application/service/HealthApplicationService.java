package com.findu.security.application.service;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Caso de uso: consulta de estado de salud de la aplicación.
 */
public interface HealthApplicationService {

    Mono<Map<String, String>> getHealth();
}
