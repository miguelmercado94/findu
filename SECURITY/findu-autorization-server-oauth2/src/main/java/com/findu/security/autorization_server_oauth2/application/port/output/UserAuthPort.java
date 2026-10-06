package com.findu.security.autorization_server_oauth2.application.port.output;

import com.findu.security.autorization_server_oauth2.domain.model.UsuarioDetails;
import reactor.core.publisher.Mono;

public interface UserAuthPort {
    Mono<UsuarioDetails> authenticate(String username, String password);
}
