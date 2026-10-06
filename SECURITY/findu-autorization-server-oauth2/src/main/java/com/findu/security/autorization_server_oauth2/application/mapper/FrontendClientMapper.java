package com.findu.security.autorization_server_oauth2.application.mapper;

import com.findu.security.autorization_server_oauth2.infrastructure.entity.FrontendClient;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.Date;

/**
 * Mapeador encargado de transformar la entidad de base de datos {@link FrontendClient} 
 * al objeto de dominio {@link RegisteredClient} requerido por Spring Authorization Server.
 * 
 * NOTA: No es ideal utilizar MapStruct para esta conversión debido a que {@link RegisteredClient} 
 * no es un POJO estándar con getters/setters ni constructor público, sino que es una clase inmutable 
 * de Spring Security que se construye estrictamente a través de su Builder personalizado y utiliza 
 * callbacks (Consumers) para inicializar sus colecciones internas. Intentar usar MapStruct aquí 
 * agregaría complejidad innecesaria (anotaciones con expresiones complejas) en lugar de simplificarlo.
 */
public class FrontendClientMapper {

    /**
     * Convierte un FrontendClient persistido a una instancia de RegisteredClient de Spring Security.
     * 
     * @param client la entidad de infraestructura FrontendClient.
     * @return el objeto RegisteredClient inicializado y listo para Spring Security.
     */
    public static RegisteredClient toRegisteredClient(FrontendClient client) {
        if (client == null) {
            return null;
        }

        // 1. Inicialización del Builder utilizando el clientId como ID y configurando metadatos básicos
        RegisteredClient.Builder builder = RegisteredClient.withId(client.getClientId())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientIdIssuedAt(new Date(System.currentTimeMillis()).toInstant());

        // 2. Mapeo de los métodos de autenticación del cliente (por ejemplo: CLIENT_SECRET_BASIC)
        if (client.getClientAuthenticationMethods() != null) {
            builder.clientAuthenticationMethods(clientAuthMethods -> {
                client.getClientAuthenticationMethods().stream()
                        .map(ClientAuthenticationMethod::new) // Crea la instancia correspondiente de Spring
                        .forEach(clientAuthMethods::add);
            });
        }

        // 3. Mapeo de los tipos de flujo de autorización permitidos (por ejemplo: AUTHORIZATION_CODE, REFRESH_TOKEN)
        if (client.getAuthorizationGrantTypes() != null) {
            builder.authorizationGrantTypes(authGrantTypes -> {
                client.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::new) // Crea la instancia correspondiente de Spring
                        .forEach(authGrantTypes::add);
            });
        }

        // 4. Mapeo de las URIs de redirección permitidas tras el login exitoso
        if (client.getRedirectUris() != null) {
            builder.redirectUris(redirectUris -> {
                client.getRedirectUris().stream().forEach(redirectUris::add);
            });
        }

        // 5. Mapeo de las URIs de redirección tras el logout (post-logout redirection)
        if (client.getPostLogoutRedirectUris() != null) {
            builder.postLogoutRedirectUris(postLogoutRedirectUris -> {
                client.getPostLogoutRedirectUris().stream().forEach(postLogoutRedirectUris::add);
            });
        }

        // 6. Mapeo de los scopes o alcances de seguridad configurados (por ejemplo: openid, read, write)
        if (client.getScopes() != null) {
            builder.scopes(scopes -> {
                client.getScopes().stream().forEach(scopes::add);
            });
        }

        // 7. Configuración de seguridad del cliente (ClientSettings), en este caso si requiere Proof Key (PKCE)
        builder.clientSettings(ClientSettings.builder()
                .requireProofKey(client.isRequiredProofKey())
                .build());

        // 8. Configuración de tiempos de vida de los tokens (TokenSettings) basándose en el parámetro durationInMinutes
        // El Refresh Token se calcula como 4 veces la duración del Access Token
        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(client.getDurationInMinutes()))
                .refreshTokenTimeToLive(Duration.ofMinutes(client.getDurationInMinutes() * 4))
                .build());

        // 9. Construcción final del objeto inmutable
        return builder.build();
    }
}
