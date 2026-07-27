package com.findu.security.autorization_server_oauth2.application.usecase;

import com.findu.security.autorization_server_oauth2.application.port.output.UserAuthPort;
import com.findu.security.autorization_server_oauth2.domain.model.UsuarioDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthenticateUserUseCase {

    private final UserAuthPort userAuthPort;

    public AuthenticateUserUseCase(UserAuthPort userAuthPort) {
        this.userAuthPort = userAuthPort;
    }

    public Mono<UsuarioDetails> authenticate(String username, String password) {
        return userAuthPort.authenticate(username, password);
    }
}
