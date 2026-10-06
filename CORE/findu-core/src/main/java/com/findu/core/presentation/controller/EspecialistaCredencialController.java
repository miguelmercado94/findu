package com.findu.core.presentation.controller;

import com.findu.core.application.usecase.GestionCredencialesUseCase;
import com.findu.core.dto.request.CrearCredencialRequest;
import com.findu.core.dto.response.EspecialistaCredencialResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/especialidades")
@RequiredArgsConstructor
@Tag(name = "Credenciales de Especialista", description = "Gestión de certificados, cursos y diplomados para acreditar conocimientos de los proveedores")
public class EspecialistaCredencialController {

    private final GestionCredencialesUseCase credencialesUseCase;

    @PostMapping("/{especialidadId}/credenciales")
    @Operation(summary = "Registrar certificado/credencial para una especialidad", description = "Registra un título SUPERIOR (requiere fechaInicio y fechaFin) o CERTIFICADO/CURSO/DIPLOMADO (requiere fechaFin)")
    @ApiResponse(responseCode = "201", description = "Credencial registrada exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EspecialistaCredencialResponse.class)))
    public ResponseEntity<EspecialistaCredencialResponse> agregarCredencial(
            @PathVariable Long especialidadId,
            @Valid @RequestBody CrearCredencialRequest request) {
        // Asegurar que coincida el especialidadId de la ruta con la del request
        CrearCredencialRequest requestFinal = new CrearCredencialRequest(
                especialidadId,
                request.tipoCertificado(),
                request.nombreTitulo(),
                request.institucion(),
                request.fechaInicio(),
                request.fechaFin(),
                request.urlCertificadoS3()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(credencialesUseCase.agregarCredencial(requestFinal));
    }

    @GetMapping("/{especialidadId}/credenciales")
    @Operation(summary = "Listar credenciales acreditadas de una especialidad")
    public ResponseEntity<List<EspecialistaCredencialResponse>> listarCredenciales(@PathVariable Long especialidadId) {
        return ResponseEntity.ok(credencialesUseCase.listarPorEspecialidad(especialidadId));
    }

    @DeleteMapping("/credenciales/{id}")
    @Operation(summary = "Eliminar una credencial o certificado registrado")
    public ResponseEntity<Void> eliminarCredencial(@PathVariable Long id) {
        credencialesUseCase.eliminarCredencial(id);
        return ResponseEntity.noContent().build();
    }
}
