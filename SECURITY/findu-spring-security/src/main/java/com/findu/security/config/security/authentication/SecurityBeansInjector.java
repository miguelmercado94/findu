package com.findu.security.config.security.authentication;

import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.JwtSignerFactory;
import com.findu.security.domain.model.JwtSignerFactoryImpl;
import com.findu.security.util.Base64Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Clase inyectora de beans de autenticación reactiva (WebFlux) para FIND-U.
 * Inicializa y configura el codificador de contraseñas, fábrica de firmas JWT,
 * cargador de detalles de usuario personalizado y el administrador de autenticación.
 */
@Configuration
public class SecurityBeansInjector {
    private static final Logger log = LoggerFactory.getLogger(SecurityBeansInjector.class);

    /**
     * Bean del codificador de contraseñas utilizando el algoritmo BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Inicializando codificador de contraseñas BCryptPasswordEncoder...");
        return new BCryptPasswordEncoder();
    }

    /**
     * Fabrica de firmas JWT basada en la clave secreta provista.
     */
    @Bean
    public JwtSignerFactory jwtSignerFactory(@Value("${jwt.secret:findu-default-secret-key-at-least-256-bits-for-hs256}") String secret) {
        log.info("Inicializando fábrica de firmas JwtSignerFactory...");
        byte[] keyBytes = decodeJwtSecret(secret);
        return new JwtSignerFactoryImpl(keyBytes);
    }

    /**
     * Decodifica jwt.secret desde Base64 estándar si viene codificado; si no, usa el string en UTF-8.
     */
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
     * Equivalente reactivo de UserDetailsService para FIND-U.
     * Carga el usuario dinámicamente buscando por:
     * - Número telefónico (si contiene el prefijo "phone:").
     * - Nombre de usuario (username).
     * - Correo electrónico (email).
     */
    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService(UsuarioService usuarioService) {
        log.info("Configurando servicio reactivo de detalles de usuario (ReactiveUserDetailsService)...");
        return principal -> {
            if (principal != null && principal.startsWith("phone:")) {
                String[] parts = principal.substring(6).split("::");
                if (parts.length == 2) {
                    log.debug("Buscando usuario por teléfono celular: pais={} número={}", parts[0], parts[1]);
                    return usuarioService.getUserByPhoneAndCountry(parts[0], parts[1])
                            .map(usuario -> (org.springframework.security.core.userdetails.UserDetails) usuario)
                            .switchIfEmpty(Mono.defer(() -> Mono.error(new UsernameNotFoundException("Usuario no encontrado por teléfono: " + principal))));
                }
            }
            log.debug("Buscando usuario por username/email: {}", principal);
            return usuarioService.getUserByUsername(principal)
                    .switchIfEmpty(usuarioService.getUserByEmail(principal))
                    .map(usuario -> (org.springframework.security.core.userdetails.UserDetails) usuario)
                    .switchIfEmpty(Mono.defer(() -> Mono.error(new UsernameNotFoundException("Usuario no encontrado: " + principal))));
        };
    }

    /**
     * Administrador de autenticación reactiva principal para FIND-U.
     * Utiliza el cargador de detalles de usuario reactivo y el codificador BCrypt.
     */
    @Bean
    @Primary
    public ReactiveAuthenticationManager reactiveAuthenticationManager(
            ReactiveUserDetailsService reactiveUserDetailsService,
            PasswordEncoder passwordEncoder) {
        log.info("Inicializando administrador de autenticación reactivo (ReactiveAuthenticationManager)...");
        var manager = new UserDetailsRepositoryReactiveAuthenticationManager(reactiveUserDetailsService);
        manager.setPasswordEncoder(passwordEncoder);
        return manager;
    }
}
