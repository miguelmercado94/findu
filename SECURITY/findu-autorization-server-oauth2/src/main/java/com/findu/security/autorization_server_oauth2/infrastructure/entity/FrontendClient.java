package com.findu.security.autorization_server_oauth2.infrastructure.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "frontend_client")
public class FrontendClient {

    @Id
    private String id;

    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;

    @Column(name = "client_id_issued_at")
    private Instant clientIdIssuedAt;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "client_secret_expires_at")
    private Instant clientSecretExpiresAt;

    @Column(name = "client_name")
    private String clientName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_auth_methods", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "method")
    private List<String> clientAuthenticationMethods;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_grant_types", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "grant_type")
    private List<String> authorizationGrantTypes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_redirect_uris", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "redirect_uri", length = 1000)
    private List<String> redirectUris;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_post_logout_redirect_uris", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "post_logout_redirect_uri", length = 1000)
    private List<String> postLogoutRedirectUris;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_scopes", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope")
    private List<String> scopes;

    @Column(name = "duration_in_minutes")
    private int durationInMinutes;

    // Ajustes del cliente
    @Column(name = "required_proof_key")
    private boolean requiredProofKey;

    @Column(name = "require_authorization_consent")
    private boolean requireAuthorizationConsent;

    // Ajustes de tokens (duración en segundos)
    @Column(name = "access_token_time_to_live_seconds")
    private Long accessTokenTimeToLiveSeconds;

    @Column(name = "refresh_token_time_to_live_seconds")
    private Long refreshTokenTimeToLiveSeconds;

    @Column(name = "reuse_refresh_tokens")
    private boolean reuseRefreshTokens;
}
