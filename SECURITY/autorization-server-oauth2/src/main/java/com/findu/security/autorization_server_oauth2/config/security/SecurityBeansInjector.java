package com.findu.security.autorization_server_oauth2.config.security;

import com.findu.security.autorization_server_oauth2.application.usecase.AuthenticateUserUseCase;
import com.findu.security.autorization_server_oauth2.domain.model.UsuarioDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Clase inyectora de beans de seguridad servlet tradicionales para el servidor de autorización OAuth2 de FIND-U.
 * Configura el PasswordEncoder, AuthenticationProvider personalizado y el AuthenticationManager.
 */
@Configuration
public class SecurityBeansInjector {
    private static final Logger log = LoggerFactory.getLogger(SecurityBeansInjector.class);

    /**
     * Bean del administrador de autenticación servlet estándar de Spring Security.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        log.info("Inicializando AuthenticationManager servlet para el Servidor de Autorización OAuth2...");
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Proveedor de autenticación personalizado que delega la autenticación
     * al caso de uso correspondiente consumiendo los adaptadores de negocio.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(AuthenticateUserUseCase authenticateUserUseCase) {
        log.info("Configurando AuthenticationProvider personalizado para validación contra base de datos FIND-U...");
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = authentication.getCredentials().toString();

                log.debug("Intento de autenticación en Servidor de Autorización para usuario: {}", username);
                try {
                    // Bloqueamos la llamada reactiva al ser un contexto Servlet/bloqueante de Spring Security MVC
                    UsuarioDetails userDetails = authenticateUserUseCase.authenticate(username, password).block();
                    if (userDetails == null) {
                        throw new BadCredentialsException("Authentication failed: User details not retrieved");
                    }
                    log.info("Usuario autenticado exitosamente en Servidor de Autorización: {}", username);
                    return new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                } catch (Exception e) {
                    log.warn("Fallo de autenticación en Servidor de Autorización para usuario: {} - razón: {}", username, e.getMessage());
                    if (e instanceof AuthenticationException) {
                        throw (AuthenticationException) e;
                    }
                    throw new BadCredentialsException("Authentication failed: " + e.getMessage(), e);
                }
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    /**
     * Bean del codificador de contraseñas utilizando el algoritmo BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Inicializando codificador de contraseñas BCryptPasswordEncoder para el Servidor de Autorización...");
        return new BCryptPasswordEncoder();
    }
}
