package com.findu.security.application.usecase;

import com.findu.security.dto.internal.InternalActivationInitRequest;
import com.findu.security.dto.internal.InternalActivationInitResponse;
import com.findu.security.dto.internal.InternalCreateUserRequest;
import com.findu.security.dto.internal.InternalTokensResponse;
import com.findu.security.dto.internal.InternalUserResponse;
import reactor.core.publisher.Mono;

public interface InternalUserManager {
    Mono<InternalUserResponse> createUser(InternalCreateUserRequest request);
    Mono<Void> deleteUser(Long userId);
    Mono<InternalTokensResponse> issueTokens(Long userId, String algorithm);
    Mono<InternalActivationInitResponse> initActivation(InternalActivationInitRequest request);
}
