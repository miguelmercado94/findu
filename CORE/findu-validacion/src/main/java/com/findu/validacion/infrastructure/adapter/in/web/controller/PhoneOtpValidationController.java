package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.request.SendPhoneOtpRequest;
import com.findu.validacion.application.dto.request.VerifyPhoneOtpRequest;
import com.findu.validacion.application.dto.response.SendPhoneOtpResponse;
import com.findu.validacion.application.dto.response.VerifyPhoneOtpResponse;
import com.findu.validacion.application.port.in.ValidatePhoneOtpUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/validations/otp")
@RequiredArgsConstructor
@Tag(name = "Phone OTP Validations", description = "Generación y verificación de OTP por SMS para procesos declarativos")
public class PhoneOtpValidationController {

    private final ValidatePhoneOtpUseCase otpUseCase;

    @Operation(summary = "Enviar OTP por SMS",
            description = "Genera un código OTP de 6 dígitos con expiración y lo envía por SMS al celular del usuario.")
    @PostMapping("/send-phone-otp")
    public Mono<ResponseEntity<SendPhoneOtpResponse>> sendPhoneOtp(
            @Valid @RequestBody SendPhoneOtpRequest request) {
        return otpUseCase.sendOtp(request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Verificar código OTP",
            description = "Valida el código OTP de 6 dígitos ingresado por el usuario para el proceso indicado.")
    @PostMapping("/verify-phone-otp")
    public Mono<ResponseEntity<VerifyPhoneOtpResponse>> verifyPhoneOtp(
            @Valid @RequestBody VerifyPhoneOtpRequest request) {
        return otpUseCase.verifyOtp(request)
                .map(ResponseEntity::ok);
    }
}
