package com.findu.security.presentation.controller;

import com.findu.security.application.usecase.CustomerManager;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.dto.request.UserRegisterDto;
import com.findu.security.dto.response.CustomerResponse;
import com.findu.security.dto.response.SaveUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.findu.security.util.SecurityConstants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API de clientes (customers).
 * Path base: /api/v1/customers.
 * Controller → CustomerManager (registro) / UsuarioService (consulta).
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Registro y consulta de clientes")
public class CustomerController {

    private final UsuarioService usuarioService;
    private final CustomerManager customerManager;

    public CustomerController(UsuarioService usuarioService, CustomerManager customerManager) {
        this.usuarioService = usuarioService;
        this.customerManager = customerManager;
    }

    /**
     * Lista todos los clientes/usuarios (sin id, password, rol ni authorities).
     * GET /api/v1/customers
     */
    @GetMapping
    @Operation(summary = "Listar clientes", description = "Obtiene usuarios registrados", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponse(responseCode = "200", description = "Lista de clientes")
    public Flux<CustomerResponse> getAllCustomers() {
        return usuarioService.findAll()
                .map(u -> new CustomerResponse(u.getUsername(), u.getEmail(), u.getPhone(), u.isEnabled()));
    }

    /**
     * Registra un nuevo cliente. Genera access + refresh JWT según el algoritmo del header.
     * POST /api/v1/customers
     * Header: X-JWT-Algorithm (ej. HS256); por defecto HS256.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar cliente", description = "Crea usuario cliente y devuelve datos + tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente creado",
                    content = @Content(schema = @Schema(implementation = SaveUserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    public Mono<SaveUserResponse> register(
            @RequestBody @Valid UserRegisterDto request,
            @Parameter(description = "Algoritmo JWT (HS256 por defecto)")
            @RequestHeader(name = SecurityConstants.HEADER_JWT_ALGORITHM, defaultValue = SecurityConstants.DEFAULT_JWT_ALGORITHM) String algorithm) {
        return customerManager.registerNewCustomer(request, algorithm);
    }
}
