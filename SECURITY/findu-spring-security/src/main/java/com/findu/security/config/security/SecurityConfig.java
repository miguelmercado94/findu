package com.findu.security.config.security;

import com.findu.security.exception.JsonAccessDeniedHandler;
import com.findu.security.exception.JsonAuthenticationEntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

/**
 * Clase de configuración principal de seguridad reactiva (WebFlux) para FIND-U.
 * Establece los filtros de seguridad, políticas de autorización personalizadas,
 * decodificación de tokens JWT y manejadores de excepciones personalizadas de entrada/salida.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${jwt.secret:findu-default-secret-key-at-least-256-bits-for-hs256}")
    private String jwtSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Define el filtro de seguridad web reactivo (SecurityWebFilterChain).
     * Configura el comportamiento stateless (sin estado), manejadores de excepciones JSON,
     * exclusión de endpoints públicos de OpenAPI/Swagger y la delegación al administrador
     * de autorización reactiva de FIND-U.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         JsonAuthenticationEntryPoint authenticationEntryPoint,
                                                         JsonAccessDeniedHandler accessDeniedHandler,
                                                         ReactiveAuthorizationManager<AuthorizationContext> authorizationManager) {
        log.info("Inicializando cadena de filtros de seguridad reactiva (SecurityWebFilterChain) para FIND-U...");
        return http
                .cors(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .anyExchange().access(authorizationManager)
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .build();
    }

    /**
     * Instancia un decodificador reactivo de tokens JWT utilizando una clave secreta HMAC-SHA256.
     * Soporta decodificación tanto de cadenas Base64 como de texto plano.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        log.info("Configurando decodificador reactivo de JWT Nimbus basado en clave secreta HMAC-SHA256");
        byte[] keyBytes = decodeSecret(jwtSecret);
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "HMACSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Decodifica la clave secreta provista por configuración.
     * Intenta resolver en formato Base64; en caso de error, recurre al formato UTF-8 nativo.
     */
    private static byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return new byte[0];
        }
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(secret.trim());
            if (decoded.length > 0) {
                return decoded;
            }
        } catch (Exception e) {
            log.debug("Clave secreta no está en formato Base64. Utilizando bytes UTF-8 planos.");
        }
        return secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Configura el convertidor reactivo de autenticación JWT.
     * Mapea la propiedad personalizada "permissions" de los claims del JWT como autoridades directas
     * sin prefijos para coincidir con el sistema de permisos de FIND-U.
     */
    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        log.debug("Configurando mapeador de autoridades JWT (claim: 'permissions')");
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("permissions");
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                new ReactiveJwtGrantedAuthoritiesConverterAdapter(jwtGrantedAuthoritiesConverter)
        );

        return jwtAuthenticationConverter;
    }
}
