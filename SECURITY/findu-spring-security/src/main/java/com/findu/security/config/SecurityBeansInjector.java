package com.findu.security.config;

import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.JwtSignerFactory;
import com.findu.security.domain.model.JwtSignerFactoryImpl;
import com.findu.security.util.Base64Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

/**
 * Beans de seguridad para WebFlux.
 * Equivalente reactivo de UserDetailsService, AuthenticationManager y AuthenticationProvider.
 */
@Configuration
public class SecurityBeansInjector {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtSignerFactory jwtSignerFactory(@Value("${jwt.secret:findu-default-secret-key-at-least-256-bits-for-hs256}") String secret) {
        byte[] keyBytes = decodeJwtSecret(secret);
        return new JwtSignerFactoryImpl(keyBytes);
    }

    /** Decodifica jwt.secret desde Base64 estándar si viene codificado; si no, usa el string en UTF-8. */
    private static byte[] decodeJwtSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return new byte[0];
        }
        byte[] decoded = Base64Util.decodeStandard(secret);
        if (decoded != null && decoded.length > 0) {
            return decoded;
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Equivalente reactivo de UserDetailsService: carga usuario por username o por email (para login con usernameOrEmail).
     */
    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService(UsuarioService usuarioService) {
        return usernameOrEmail -> usuarioService.getUserByUsername(usernameOrEmail)
                .switchIfEmpty(usuarioService.getUserByEmail(usernameOrEmail))
                .map(usuario -> (org.springframework.security.core.userdetails.UserDetails) usuario)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new UsernameNotFoundException("User not found: " + usernameOrEmail))));
    }

    /**
     * Equivalente reactivo de AuthenticationManager: valida credenciales con ReactiveUserDetailsService y PasswordEncoder.
     * @Primary para que Spring use este cuando se requiera un único bean (p. ej. configuración por defecto del filtro).
     */
    @Bean
    @Primary
    public ReactiveAuthenticationManager reactiveAuthenticationManager(
            ReactiveUserDetailsService reactiveUserDetailsService,
            PasswordEncoder passwordEncoder) {
        var manager = new UserDetailsRepositoryReactiveAuthenticationManager(reactiveUserDetailsService);
        manager.setPasswordEncoder(passwordEncoder);
        return manager;
    }
}
