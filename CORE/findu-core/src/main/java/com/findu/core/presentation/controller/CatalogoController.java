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
    @Operation(summary = "Listar categorías activas", description = "Retorna todas las categorías de servicio que se encuentran activas en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de categorías obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoriaResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        List<CategoriaResponse> response = catalogoService.findAllCategorias().stream()
                .map(this::toCategoriaResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/servicios")
    @Operation(summary = "Listar servicios por categoría", description = "Retorna los servicios disponibles filtrados por categoría")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de servicios obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ServicioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetro de categoría inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<List<ServicioResponse>> listarServicios(
            @Parameter(description = "ID de la categoría para filtrar servicios", example = "1")
            @RequestParam(required = false) Long categoriaId) {
        List<Servicio> servicios = categoriaId != null
                ? catalogoService.findServiciosByCategoria(categoriaId)
                : List.of();
        List<ServicioResponse> response = servicios.stream()
                .map(s -> new ServicioResponse(s.getId(), s.getNombre(), s.getDescripcion(), s.getTipoCobro()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/direcciones/municipios")
    @Operation(summary = "Listar municipios activos", description = "Retorna todos los municipios disponibles para registro de direcciones")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de municipios obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MunicipioResponse.class))),
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
