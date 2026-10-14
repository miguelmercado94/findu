package com.findu.security.presentation.controller;

import com.findu.security.application.service.UsuarioService;
import com.findu.security.dto.response.CheckAvailabilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Endpoints públicos de validación de disponibilidad para el stepper de registro / onboarding.
 * Registrados con permite_all=true en la tabla operation.
 */
@RestController
@RequestMapping("/api/v1/registration")
@Tag(name = "Registration Checks", description = "Validaciones de disponibilidad en vivo (email, username, teléfono)")
public class RegistrationCheckController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String DEFAULT_COUNTRY = "+57";

    private final UsuarioService usuarioService;

    public RegistrationCheckController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/check-email")
    @Operation(summary = "Verificar disponibilidad de correo electrónico")
    public Mono<ResponseEntity<Object>> checkEmail(@RequestParam(name = "email", required = false) String email) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_EMAIL_FORMAT")));
        }
        return usuarioService.existsByEmail(email.trim().toLowerCase())
                .map(exists -> exists
                        ? ResponseEntity.ok(CheckAvailabilityResponse.ofUnavailable("EMAIL_TAKEN"))
                        : ResponseEntity.ok(CheckAvailabilityResponse.ofAvailable()));
    }

    @GetMapping("/check-username")
    @Operation(summary = "Verificar disponibilidad de nombre de usuario")
    public Mono<ResponseEntity<Object>> checkUsername(@RequestParam(name = "username", required = false) String username) {
        if (username == null || username.isBlank() || username.trim().length() < 3) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_USERNAME_FORMAT")));
        }
        return usuarioService.existsByUsername(username.trim())
                .map(exists -> exists
                        ? ResponseEntity.ok(CheckAvailabilityResponse.ofUnavailable("USERNAME_TAKEN"))
                        : ResponseEntity.ok(CheckAvailabilityResponse.ofAvailable()));
    }

    @GetMapping("/check-phone")
    @Operation(summary = "Verificar disponibilidad de teléfono")
    public Mono<ResponseEntity<Object>> checkPhone(
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "codPhoneInternational", required = false, defaultValue = DEFAULT_COUNTRY) String codPhoneInternational) {
        if (phone == null || phone.isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_PHONE_FORMAT")));
        }
        String country = (codPhoneInternational != null && !codPhoneInternational.isBlank())
                ? codPhoneInternational.trim() : DEFAULT_COUNTRY;
        return usuarioService.getUserByPhoneAndCountry(country, phone.trim())
                .map(user -> ResponseEntity.ok((Object) CheckAvailabilityResponse.ofUnavailable("PHONE_TAKEN")))
                .defaultIfEmpty(ResponseEntity.ok(CheckAvailabilityResponse.ofAvailable()));
    }
}
