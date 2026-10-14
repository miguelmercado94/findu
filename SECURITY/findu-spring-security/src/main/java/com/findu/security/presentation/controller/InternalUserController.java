package com.findu.security.presentation.controller;

import com.findu.security.application.usecase.InternalUserManager;
import com.findu.security.dto.internal.InternalActivationInitRequest;
import com.findu.security.dto.internal.InternalActivationInitResponse;
import com.findu.security.dto.internal.InternalCreateUserRequest;
import com.findu.security.dto.internal.InternalTokensResponse;
import com.findu.security.dto.internal.InternalUserResponse;
import com.findu.security.util.SecurityConstants;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Hidden
@RestController
@RequestMapping("/internal")
public class InternalUserController {

    private static final Logger log = LoggerFactory.getLogger(InternalUserController.class);
    private static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";

    private final InternalUserManager internalUserManager;

    @Value("${findu.internal.service-key:findu-internal-saga-secret-key-2026}")
    private String expectedInternalKey;

    public InternalUserController(InternalUserManager internalUserManager) {
        this.internalUserManager = internalUserManager;
    }

    private void validateInternalAccess(String headerKey) {
        if (headerKey == null || !headerKey.equals(expectedInternalKey)) {
            log.warn("Access to /internal endpoint denied due to invalid or missing service key");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso interno no autorizado");
        }
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<InternalUserResponse>> createUser(
            @RequestHeader(name = HEADER_INTERNAL_KEY, required = false) String serviceKey,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody InternalCreateUserRequest request) {
        validateInternalAccess(serviceKey);
        return internalUserManager.createUser(request, idempotencyKey)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res));
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(
            @RequestHeader(name = HEADER_INTERNAL_KEY, required = false) String serviceKey,
            @PathVariable Long userId) {
        validateInternalAccess(serviceKey);
        return internalUserManager.deleteUser(userId);
    }

    @PostMapping("/users/{userId}/tokens")
    public Mono<ResponseEntity<InternalTokensResponse>> issueTokens(
            @RequestHeader(name = HEADER_INTERNAL_KEY, required = false) String serviceKey,
            @RequestHeader(name = SecurityConstants.HEADER_JWT_ALGORITHM, defaultValue = SecurityConstants.DEFAULT_JWT_ALGORITHM) String algorithm,
            @PathVariable Long userId) {
        validateInternalAccess(serviceKey);
        return internalUserManager.issueTokens(userId, algorithm)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/activation/init")
    public Mono<ResponseEntity<InternalActivationInitResponse>> initActivation(
            @RequestHeader(name = HEADER_INTERNAL_KEY, required = false) String serviceKey,
            @RequestBody InternalActivationInitRequest request) {
        validateInternalAccess(serviceKey);
        return internalUserManager.initActivation(request)
                .map(ResponseEntity::ok);
    }
}
