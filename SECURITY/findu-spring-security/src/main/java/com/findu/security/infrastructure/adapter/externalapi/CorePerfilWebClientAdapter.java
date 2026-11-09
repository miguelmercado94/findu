package com.findu.security.infrastructure.adapter.externalapi;

import com.findu.security.application.port.output.externalapi.CorePerfilPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter WebClient hacia findu-core (perfil-proveedor), a través del gateway.
 * El fetch devuelve {@code Mono.empty()} en 404 (perfil no existe aún). El PUT de foto es best-effort.
 */
@Component
public class CorePerfilWebClientAdapter implements CorePerfilPort {

    private static final Logger log = LoggerFactory.getLogger(CorePerfilWebClientAdapter.class);

    private final WebClient webClient;
    private static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";
    private final String perfilPath;
    private final String internalPerfilesPath;
    private final String internalServiceKey;

    public CorePerfilWebClientAdapter(
            @Value("${findu.activation.core.base-url:http://findu-api-gateway:8080}") String baseUrl,
            @Value("${findu.activation.core.perfil-path:/findu-core/api/v1/perfil-proveedor}") String perfilPath,
            @Value("${findu.activation.core.internal-perfiles-path:/findu-core/internal/perfiles}") String internalPerfilesPath,
            @Value("${findu.internal.service-key:findu-internal-saga-secret-key-2026}") String internalServiceKey) {
        this.internalPerfilesPath = internalPerfilesPath;
        this.internalServiceKey = internalServiceKey;
        this.perfilPath = perfilPath;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /** Vista parcial de la respuesta de core (solo lo que necesitamos). */
    private record CorePerfilResponse(Long id, String nombreCompleto, LocalDate fechaNacimiento, String urlImagenPerfil) {}

    @Override
    public Mono<PerfilProveedor> fetchByAuthUserId(Long authUserId) {
        return webClient.get()
                .uri(perfilPath + "/auth/{authUserId}", authUserId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp -> Mono.empty())
                .bodyToMono(CorePerfilResponse.class)
                .map(r -> new PerfilProveedor(r.id(), r.nombreCompleto(), r.fechaNacimiento(), r.urlImagenPerfil()))
                .onErrorResume(e -> {
                    log.warn("No se pudo obtener el perfil de core (authUserId={}): {}", authUserId, e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> pushProfilePhoto(Long perfilId, String nombreCompleto, String urlImagenPerfil) {
        Map<String, Object> body = new HashMap<>();
        body.put("nombreCompleto", nombreCompleto);
        body.put("urlImagenPerfil", urlImagenPerfil);
        return webClient.put()
                .uri(perfilPath + "/{id}", perfilId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.warn("Propagación de foto a core falló (perfilId={}, best-effort): {}", perfilId, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    @Override
    public Mono<Void> pushBiometricEvidence(Long perfilId, String rostroBase64, String documentoFrenteBase64,
                                            String documentoReversoBase64, BigDecimal faceMatchScore) {
        Map<String, Object> body = new HashMap<>();
        body.put("rostroBase64", rostroBase64);
        body.put("documentoFrenteBase64", documentoFrenteBase64);
        body.put("documentoReversoBase64", documentoReversoBase64);
        body.put("faceMatchScore", faceMatchScore);
        return webClient.post()
                .uri(internalPerfilesPath + "/{id}/biometria", perfilId)
                .header(HEADER_INTERNAL_KEY, internalServiceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
