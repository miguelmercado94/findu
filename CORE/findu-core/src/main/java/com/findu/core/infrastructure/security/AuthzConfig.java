package com.findu.core.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

@Configuration
public class AuthzConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthzConfig.class);

    /** Secretos por defecto publicados en el repositorio (UTF-8 y su Base64 usado en docker-compose). */
    private static final String KNOWN_DEFAULT = "findu-default-secret-key-at-least-256-bits-for-hs256";
    private static final Set<String> KNOWN_DEFAULTS = Set.of(
            KNOWN_DEFAULT,
            Base64.getEncoder().encodeToString(KNOWN_DEFAULT.getBytes(StandardCharsets.UTF_8)));

    @Bean
    public JwtVerifier jwtVerifier(AuthzProperties props) {
        String secret = props.getJwtSecret();
        if (props.getMode() != AuthzMode.OFF) {
            if (secret == null || secret.isBlank()) {
                log.warn("findu.security.authz.mode={} pero JWT_SECRET esta vacio: ningun token sera valido",
                        props.getMode());
            } else if (KNOWN_DEFAULTS.contains(secret.trim())) {
                log.warn("findu.security.authz.mode={} usa el JWT_SECRET por defecto del repositorio (publico): "
                        + "defina un secreto unico por entorno", props.getMode());
            }
        }
        return new JwtVerifier(secret);
    }
}
