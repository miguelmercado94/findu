package com.findu.security.presentation.controller;

import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.CustomerManager;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.request.UserRegisterDto;
import com.findu.security.dto.response.SaveUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private CustomerManager customerManager;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new CustomerController(usuarioService, customerManager)).build();
    }

    @Test
    void getAllCustomers_returnsList() {
        Usuario u = new Usuario();
        u.setUsername("a");
        u.setEmail("e@e.com");
        u.setPhone("1");
        u.setActive(true);
        when(usuarioService.findAll()).thenReturn(Flux.just(u));

        client.get().uri("/api/v1/customers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].username").isEqualTo("a");
    }

    @Test
    void register_returns201() {
        var dto = new UserRegisterDto("u", "u@e.com", "1", "+57", "p", "ROLE_CUSTOMER");
        when(customerManager.registerNewCustomer(any(), eq("HS256")))
                .thenReturn(Mono.just(new SaveUserResponse("u", "u@e.com", "1", "ROLE_CUSTOMER", java.util.List.of(), "jwt", "ref")));

        client.post().uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-JWT-Algorithm", "HS256")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated();
    }
}
