package com.findu.security.application.service.impl;

import com.findu.security.application.service.HealthApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class HealthApplicationServiceImpl implements HealthApplicationService {

    @Value("${spring.application.name:findu-spring-security}")
    private String applicationName;

    @Override
    public Mono<Map<String, String>> getHealth() {
        return Mono.just(Map.of(
                "status", "UP",
                "application", applicationName
        ));
    }
}
