package com.findu.security.autorization_server_oauth2.infrastructure.adapter.rest;

import com.findu.security.autorization_server_oauth2.application.port.output.UserAuthPort;
import com.findu.security.autorization_server_oauth2.domain.model.UsuarioDetails;
import com.findu.security.autorization_server_oauth2.infrastructure.adapter.rest.dto.AuthToken;
import com.findu.security.autorization_server_oauth2.infrastructure.adapter.rest.dto.LoginRequest;
import com.findu.security.autorization_server_oauth2.infrastructure.adapter.rest.dto.ValidateTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class UserAuthRestAdapter implements UserAuthPort {

    private final WebClient webClient;

    public UserAuthRestAdapter(@Value("${findu.security-api.base-url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Mono<UsuarioDetails> authenticate(String username, String password) {
        String clientId = getClientIdFromSession();
        String role = determineRole(clientId);
        LoginRequest loginRequest = new LoginRequest(username, password, role);

        return webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .retrieve()
                .bodyToMono(AuthToken.class)
                .flatMap(authToken -> {
                    if (authToken == null || !authToken.available() || authToken.jwt() == null || authToken.jwt().isBlank()) {
                        return Mono.error(new BadCredentialsException("Invalid credentials"));
                    }
                    return validateTokenAndFetchProfile(authToken.jwt());
                })
                .onErrorMap(throwable -> {
                    if (throwable instanceof BadCredentialsException) {
                        return throwable;
                    }
                    return new BadCredentialsException("Authentication failed: " + throwable.getMessage(), throwable);
                });
    }

    private String getClientIdFromSession() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    SavedRequest savedRequest = (SavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
                    if (savedRequest != null) {
                        String[] clientIds = savedRequest.getParameterValues("client_id");
                        if (clientIds != null && clientIds.length > 0) {
                            return clientIds[0];
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignored
        }
        return null;
    }

    private String determineRole(String clientId) {
        if ("findu-admin".equals(clientId)) {
            return "ROLE_ADMINISTRATOR";
        } else if ("findu-proveedor".equals(clientId)) {
            return "ROLE_OUR_PROVEEDOR";
        } else {
            return "ROLE_OUR_CLIENTE";
        }
    }

    private Mono<UsuarioDetails> validateTokenAndFetchProfile(String jwt) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/auth/validate")
                        .queryParam("token", jwt)
                        .build())
                .retrieve()
                .bodyToMono(ValidateTokenResponse.class)
                .map(response -> {
                    if (response == null || !response.tokenValid() || response.payload() == null) {
                        throw new BadCredentialsException("Token validation failed");
                    }

                    Map<String, Object> payload = response.payload();
                    String username = (String) payload.getOrDefault("sub", payload.get("name"));
                    String role = (String) payload.get("role");

                    @SuppressWarnings("unchecked")
                    List<String> authorities = (List<String>) payload.getOrDefault("authorities", Collections.emptyList());

                    return UsuarioDetails.builder()
                            .username(username)
                            .role(role)
                            .permissions(authorities)
                            .build();
                });
    }
}
