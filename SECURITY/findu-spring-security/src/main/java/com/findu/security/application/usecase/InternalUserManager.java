package com.findu.security.application.usecase;

import com.findu.security.dto.internal.InternalActivationInitRequest;
import com.findu.security.dto.internal.InternalActivationInitResponse;
import com.findu.security.dto.internal.InternalCreateUserRequest;
import com.findu.security.dto.internal.InternalTokensResponse;
import com.findu.security.dto.internal.InternalUserResponse;
import reactor.core.publisher.Mono;

public interface InternalUserManager {
    default Mono<InternalUserResponse> createUser(InternalCreateUserRequest request) {
        return createUser(request, null);
    }
    Mono<InternalUserResponse> createUser(InternalCreateUserRequest request, String idempotencyKey);
    Mono<Void> deleteUser(Long userId);
    Mono<InternalTokensResponse> issueTokens(Long userId, String algorithm);
    Mono<InternalActivationInitResponse> initActivation(InternalActivationInitRequest request);
}
