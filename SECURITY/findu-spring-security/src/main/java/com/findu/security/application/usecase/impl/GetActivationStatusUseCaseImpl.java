package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.service.AuthenticatedUserResolver;
import com.findu.security.application.usecase.GetActivationStatusUseCase;
import com.findu.security.dto.response.ActivationStatusResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GetActivationStatusUseCaseImpl implements GetActivationStatusUseCase {

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final ProviderActivationRepositoryPort activationRepository;

    public GetActivationStatusUseCaseImpl(AuthenticatedUserResolver authenticatedUserResolver,
                                          ProviderActivationRepositoryPort activationRepository) {
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.activationRepository = activationRepository;
    }

    @Override
    public Mono<ActivationStatusResponse> getStatus() {
        return authenticatedUserResolver.resolve()
                .flatMap(user -> activationRepository.loadByUserId(user.getId()))
                .map(ActivationStatusResponse::from);
    }
}
