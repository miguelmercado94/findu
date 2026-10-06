package com.findu.security.autorization_server_oauth2.application.service;

import com.findu.security.autorization_server_oauth2.application.mapper.FrontendClientMapper;
import com.findu.security.autorization_server_oauth2.exception.ObjectNotFoundException;
import com.findu.security.autorization_server_oauth2.infrastructure.entity.FrontendClient;
import com.findu.security.autorization_server_oauth2.infrastructure.repository.FrontendClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

@Service
public class RegisteredClientService implements RegisteredClientRepository {

    private final FrontendClientRepository frontendClientRepository;

    public RegisteredClientService(FrontendClientRepository frontendClientRepository) {
        this.frontendClientRepository = frontendClientRepository;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        // No soportado de forma dinámica por ahora
    }

    @Override
    public RegisteredClient findById(String id) {
        FrontendClient clientApp = frontendClientRepository.findByClientId(id)
                .orElseThrow(() -> new ObjectNotFoundException("Client not found"));
        return FrontendClientMapper.toRegisteredClient(clientApp);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        FrontendClient clientApp = frontendClientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ObjectNotFoundException("Client not found"));
        return FrontendClientMapper.toRegisteredClient(clientApp);
    }
}
