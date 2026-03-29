package com.findu.security.presentation.controller;

import com.findu.security.application.service.HealthApplicationService;
import com.findu.security.dto.response.HealthResponse;
import com.findu.security.mapper.HealthMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Health", description = "Estado de salud de la API")
public class HealthController {

    private final HealthApplicationService healthApplicationService;

    public HealthController(HealthApplicationService healthApplicationService) {
        this.healthApplicationService = healthApplicationService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Endpoint publico de estado")
    @ApiResponse(responseCode = "200", description = "Estado de salud",
            content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    public Mono<HealthResponse> health() {
        return healthApplicationService.getHealth()
                .map(HealthMapper::toResponse);
    }
}
