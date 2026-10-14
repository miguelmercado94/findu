package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilClienteRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/registration")
@RequiredArgsConstructor
@Tag(name = "Registration Checks Core", description = "Validaciones de disponibilidad de documento de identidad en core")
public class RegistrationCheckCoreController {

    private final PerfilProveedorRepository proveedorRepository;
    private final PerfilClienteRepository clienteRepository;

    @GetMapping("/check-document")
    @Operation(summary = "Verificar disponibilidad de documento de identidad")
    public ResponseEntity<?> checkDocument(
            @RequestParam(name = "tipoDocumento", required = false) String tipoDocumento,
            @RequestParam(name = "numero", required = false) String numero) {
        if (numero == null || numero.isBlank() || tipoDocumento == null || tipoDocumento.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_DOCUMENT_FORMAT"));
        }

        String trimmedNum = numero.trim();
        boolean takenInProveedor = proveedorRepository.existsByNumeroIdentificacion(trimmedNum);
        boolean takenInCliente = clienteRepository.existsByNumeroIdentificacion(trimmedNum);

        if (takenInProveedor || takenInCliente) {
            return ResponseEntity.ok(Map.of("available", false, "reason", "DOCUMENT_TAKEN"));
        }

        return ResponseEntity.ok(Map.of("available", true));
    }
}
