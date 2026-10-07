package com.findu.security.presentation.controller;

import com.findu.security.application.usecase.SendEmailVerificationUseCase;
import com.findu.security.application.usecase.SendPhoneOtpUseCase;
import com.findu.security.application.usecase.VerifyEmailUseCase;
import com.findu.security.application.usecase.VerifyPhoneOtpUseCase;
import com.findu.security.dto.request.SendEmailVerificationRequest;
import com.findu.security.dto.request.SendPhoneOtpRequest;
import com.findu.security.dto.request.VerifyPhoneOtpRequest;
import com.findu.security.dto.response.PhoneVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Verificación de identidad del proveedor: OTP de teléfono (síncrono) y enlace mágico de correo (asíncrono).
 * Path base: /api/v1/auth. Las 4 operaciones son públicas (permite_all en BD); ver docs/verificacion-identidad-proveedor.md.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Provider Verification", description = "Verificación de teléfono (OTP) y correo (enlace) del proveedor")
public class ProviderVerificationController {

    private final SendPhoneOtpUseCase sendPhoneOtpUseCase;
    private final VerifyPhoneOtpUseCase verifyPhoneOtpUseCase;
    private final SendEmailVerificationUseCase sendEmailVerificationUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;

    public ProviderVerificationController(SendPhoneOtpUseCase sendPhoneOtpUseCase,
                                          VerifyPhoneOtpUseCase verifyPhoneOtpUseCase,
                                          SendEmailVerificationUseCase sendEmailVerificationUseCase,
                                          VerifyEmailUseCase verifyEmailUseCase) {
        this.sendPhoneOtpUseCase = sendPhoneOtpUseCase;
        this.verifyPhoneOtpUseCase = verifyPhoneOtpUseCase;
        this.sendEmailVerificationUseCase = sendEmailVerificationUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
    }

    @PostMapping("/phone/send-otp")
    @Operation(summary = "Enviar OTP de teléfono", description = "Genera y envía un OTP de 6 dígitos al teléfono del proveedor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada")
    })
    public Mono<Void> sendPhoneOtp(@RequestBody @Valid SendPhoneOtpRequest request) {
        return sendPhoneOtpUseCase.sendOtp(request.phone(), request.codPhoneInternational());
    }

    @PostMapping("/phone/verify-otp")
    @Operation(summary = "Verificar OTP de teléfono", description = "Valida el OTP y marca el teléfono como verificado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado de verificación",
                    content = @Content(schema = @Schema(implementation = PhoneVerificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Código inválido o expirado")
    })
    public Mono<PhoneVerificationResponse> verifyPhoneOtp(@RequestBody @Valid VerifyPhoneOtpRequest request) {
        return verifyPhoneOtpUseCase.verifyOtp(request.phone(), request.code(), request.codPhoneInternational());
    }

    @PostMapping("/email/send-verification")
    @Operation(summary = "Enviar verificación de correo", description = "Genera y envía el enlace mágico de verificación de correo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada")
    })
    public Mono<Void> sendEmailVerification(@RequestBody @Valid SendEmailVerificationRequest request) {
        return sendEmailVerificationUseCase.sendVerification(request.email());
    }

    @GetMapping(value = "/email/verify", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Verificar correo", description = "Endpoint público abierto desde el enlace del correo; valida el token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página HTML de éxito o fallo")
    })
    public Mono<String> verifyEmail(@RequestParam("token") String token) {
        return verifyEmailUseCase.verify(token)
                .map(ProviderVerificationController::resultPage);
    }

    private static String resultPage(boolean success) {
        String title = success ? "Correo verificado" : "No pudimos verificar tu correo";
        String message = success
                ? "Tu correo ha sido confirmado. Ya puedes continuar en la aplicación FIND-U."
                : "El enlace es inválido o expiró. Solicita un nuevo correo de verificación desde la aplicación.";
        String color = success ? "#2e7d32" : "#c62828";
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>" + title + "</title></head>"
                + "<body style=\"font-family:Arial,Helvetica,sans-serif;background:#f5f5f5;margin:0;padding:40px;\">"
                + "<div style=\"max-width:480px;margin:0 auto;background:#fff;border-radius:8px;padding:32px;text-align:center;"
                + "box-shadow:0 2px 8px rgba(0,0,0,0.08);\">"
                + "<h1 style=\"color:" + color + ";font-size:22px;\">" + title + "</h1>"
                + "<p style=\"color:#444;font-size:15px;line-height:1.5;\">" + message + "</p>"
                + "</div></body></html>";
    }
}
