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
 * Endpoints de productos (simulación). Sin repo ni modelo.
 * Para futura securización: Admin (CRUD + disable), Assistant (read + update).
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Endpoints simulados de productos")
public class ProductController {

    @GetMapping
    @Operation(summary = "Listar productos", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> readProducts() {
        return Mono.just(Map.of(
                "data", List.of(
                        Map.of("id", 1L, "name", "Producto simulado 1", "active", true),
                        Map.of("id", 2L, "name", "Producto simulado 2", "active", true)
                ),
                "message", "Simulación: lista de productos"
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar producto por id", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> readOneProduct(@PathVariable Long id) {
        return Mono.just(Map.of(
                "id", id,
                "name", "Producto simulado " + id,
                "active", true,
                "message", "Simulación: producto por id"
        ));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear producto (simulado)", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> createProduct(@RequestBody Map<String, Object> body) {
        return Mono.just(Map.of(
                "id", 99L,
                "name", body.getOrDefault("name", "Nuevo producto"),
                "active", true,
                "message", "Simulación: producto creado"
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto (simulado)", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Mono.just(Map.of(
                "id", id,
                "name", body.getOrDefault("name", "Producto actualizado"),
                "active", body.getOrDefault("active", true),
                "message", "Simulación: producto actualizado"
        ));
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Deshabilitar producto (simulado)", security = {@SecurityRequirement(name = "bearerAuth")})
    public Mono<Map<String, Object>> disableProduct(@PathVariable Long id) {
        return Mono.just(Map.of(
                "id", id,
                "active", false,
                "message", "Simulación: producto deshabilitado"
        ));
    }
}
