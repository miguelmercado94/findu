package com.findu.security.autorization_server_oauth2.infrastructure.adapter.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String usernameOrEmail;
    private String password;
    private String role;
}
