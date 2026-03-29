package com.findu.security.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de categorías (simulación). Sin repo ni modelo.
 * Para futura securización: Admin (CRUD + disable), Assistant (read + update).
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Endpoints simulados de categorias")
public class CategoryController {

    @GetMapping
    @Operation(summary = "Listar categorias", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> readCategories() {
        return Mono.just(Map.of(
                "data", List.of(
                        Map.of("id", 1L, "name", "Categoría simulada 1", "active", true),
                        Map.of("id", 2L, "name", "Categoría simulada 2", "active", true)
                ),
                "message", "Simulación: lista de categorías"
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar categoria por id", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> readOneCategory(@PathVariable Long id) {
        return Mono.just(Map.of(
                "id", id,
                "name", "Categoría simulada " + id,
                "active", true,
                "message", "Simulación: categoría por id"
        ));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear categoria (simulado)", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> createCategory(@RequestBody Map<String, Object> body) {
        return Mono.just(Map.of(
                "id", 99L,
                "name", body.getOrDefault("name", "Nueva categoría"),
                "active", true,
                "message", "Simulación: categoría creada"
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoria (simulado)", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Mono.just(Map.of(
                "id", id,
                "name", body.getOrDefault("name", "Categoría actualizada"),
                "active", body.getOrDefault("active", true),
                "message", "Simulación: categoría actualizada"
        ));
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Deshabilitar categoria (simulado)", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> disableCategory(@PathVariable Long id) {
        return Mono.just(Map.of(
                "id", id,
                "active", false,
                "message", "Simulación: categoría deshabilitada"
        ));
    }
}
