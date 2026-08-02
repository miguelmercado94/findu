package com.findu.core.presentation.controller;

import com.findu.core.application.service.CatalogoService;
import com.findu.core.domain.model.Categoria;
import com.findu.core.domain.model.Municipio;
import com.findu.core.domain.model.Servicio;
import com.findu.core.dto.response.CategoriaResponse;
import com.findu.core.dto.response.MunicipioResponse;
import com.findu.core.dto.response.ServicioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Catálogos", description = "Consulta de categorías, servicios y municipios")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorías principales (paginado)", description = "Retorna categorías raíz (sin padre) paginadas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de categorías obtenida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Page<CategoriaResponse>> listarCategorias(
            @Parameter(description = "Número de página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CategoriaResponse> response = catalogoService.findCategorias(pageable)
                .map(this::toCategoriaResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categorias/{id}/subcategorias")
    @Operation(summary = "Listar subcategorías (paginado)", description = "Retorna las subcategorías de una categoría padre")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de subcategorías obtenida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Page<CategoriaResponse>> listarSubcategorias(
            @Parameter(description = "ID de la categoría padre", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Número de página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CategoriaResponse> response = catalogoService.findSubcategorias(id, pageable)
                .map(this::toCategoriaResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/servicios")
    @Operation(summary = "Listar servicios (paginado)", description = "Retorna servicios disponibles, opcionalmente filtrados por categoría")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de servicios obtenida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Page<ServicioResponse>> listarServicios(
            @Parameter(description = "ID de la categoría para filtrar", example = "1")
            @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "Número de página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ServicioResponse> response = catalogoService.findServicios(categoriaId, pageable)
                .map(s -> new ServicioResponse(s.getId(), s.getNombre(), s.getDescripcion(), s.getTipoCobro()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/direcciones/municipios")
    @Operation(summary = "Listar municipios activos", description = "Retorna todos los municipios disponibles para registro de direcciones")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de municipios obtenida exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<List<MunicipioResponse>> listarMunicipios() {
        List<MunicipioResponse> response = catalogoService.findAllMunicipios().stream()
                .map(m -> new MunicipioResponse(m.getId(), m.getCodigoDane(), m.getNombre(), m.getDepartamento()))
                .toList();
        return ResponseEntity.ok(response);
    }

    private CategoriaResponse toCategoriaResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), List.of());
    }
}
