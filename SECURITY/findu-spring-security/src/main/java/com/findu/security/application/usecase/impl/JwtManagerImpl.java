package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.application.service.JwtService;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.JwtManager;
import com.findu.security.domain.model.Jwt;
import com.findu.security.domain.model.JwtHeader;
import com.findu.security.domain.model.JwtPayload;
import com.findu.security.domain.model.JwtSignerFactory;
import com.findu.security.domain.model.Usuario;
import com.findu.security.application.service.JwtTokenRevocationService;
import com.findu.security.dto.request.LoginRequest;
import com.findu.security.dto.request.LogoutRequest;
import com.findu.security.domain.model.JwtClaims;
import com.findu.security.dto.response.AuthToken;
import com.findu.security.dto.response.UserProfileResponse;
import com.findu.security.dto.response.ValidateTokenResponse;
import com.findu.security.exception.ResourceNotFoundException;
import com.findu.security.util.ReactiveUserAuthoritiesLoader;
import com.findu.security.util.SecurityConstants;
import com.findu.security.util.UserOperationNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del caso de uso JwtManager: login (username/email + password), refresh y logout.
 */
@Service
public class JwtManagerImpl implements JwtManager {
    private static final Logger log = LoggerFactory.getLogger(JwtManagerImpl.class);

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final JwtSignerFactory jwtSignerFactory;
    private final RolRepositoryPort rolRepositoryPort;
    private final UserRolRepositoryPort userRolRepositoryPort;
    private final RolOperationRepositoryPort rolOperationRepositoryPort;
    private final ReactiveAuthenticationManager reactiveAuthenticationManager;
    private final JwtTokenRevocationService jwtTokenRevocationService;

    @Value("${jwt.access-expiration-seconds:300}")
    private long accessExpirationSeconds;

    @Value("${jwt.refresh-expiration-seconds:604800}")
    private long refreshExpirationSeconds;

    public JwtManagerImpl(UsuarioService usuarioService,
                          JwtService jwtService,
                          JwtSignerFactory jwtSignerFactory,
                          RolRepositoryPort rolRepositoryPort,
                          UserRolRepositoryPort userRolRepositoryPort,
                          RolOperationRepositoryPort rolOperationRepositoryPort,
                          ReactiveAuthenticationManager reactiveAuthenticationManager,
                          JwtTokenRevocationService jwtTokenRevocationService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.jwtSignerFactory = jwtSignerFactory;
        this.rolRepositoryPort = rolRepositoryPort;
        this.userRolRepositoryPort = userRolRepositoryPort;
        this.rolOperationRepositoryPort = rolOperationRepositoryPort;
        this.reactiveAuthenticationManager = reactiveAuthenticationManager;
        this.jwtTokenRevocationService = jwtTokenRevocationService;
    }

    @Override
    public Mono<AuthToken> login(LoginRequest loginRequest, String algorithm) {
        String alg = normalizeAlgorithm(algorithm);
        String roleName = loginRequest.role() != null ? loginRequest.role().trim() : "";
        String usernameOrEmail = loginRequest.usernameOrEmail().trim();
        log.info("Login attempt userOrEmail={} role={} alg={}", usernameOrEmail, roleName, alg);
        Mono<Void> validateRole = rolRepositoryPort.findByName(roleName)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rol no encontrado: " + roleName)))
                .then();
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                usernameOrEmail,
                loginRequest.password()
        );
        return validateRole
                .then(reactiveAuthenticationManager.authenticate(authToken))
                .map(auth -> (Usuario) auth.getPrincipal())
                .flatMap(this::enrichUserWithRole)
                .flatMap(this::enrichUserWithAuthorities)
                .flatMap(user -> buildAuthToken(user, alg)
                        .doOnSuccess(tokens -> log.info("Login successful user={} role={}", user.getUsername(),
                                user.getRol() != null ? user.getRol().getName() : null)));
    }

    @Override
    public Mono<AuthToken> refresh(String refreshToken, String algorithm) {
        log.info("Refresh token flow started");
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new IllegalArgumentException("Refresh token requerido"));
        }
        if (!jwtService.isValid(refreshToken)) {
            return Mono.error(new IllegalArgumentException("Refresh token inválido o expirado"));
        }
        if (!jwtService.isRefreshToken(refreshToken)) {
            return Mono.error(new IllegalArgumentException(
                    "Token inválido: en este endpoint debe enviar el refresh token, no el access token"));
        }
        return jwtTokenRevocationService.isRevoked(refreshToken)
                .flatMap(revoked -> {
                    if (Boolean.TRUE.equals(revoked)) {
                        return Mono.error(new IllegalArgumentException("Refresh token revocado"));
                    }
                    return refreshAfterRevocationCheck(refreshToken, algorithm);
                });
    }

    private Mono<AuthToken> refreshAfterRevocationCheck(String refreshToken, String algorithm) {
        String alg = normalizeAlgorithm(algorithm);
        var claims = jwtService.parse(refreshToken);
        String sub = claims.getSub();
        log.debug("Refresh token parsed sub={}", sub);
        if (sub == null || sub.isBlank()) {
            return Mono.error(new IllegalArgumentException("Refresh token sin subject"));
        }
        return usuarioService.getUserByUsername(sub)
                .switchIfEmpty(usuarioService.getUserByEmail(sub))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")))
                .flatMap(this::enrichUserWithRole)
                .flatMap(this::enrichUserWithAuthorities)
                .flatMap(user -> buildAuthToken(user, alg)
                        .flatMap(tokens -> jwtTokenRevocationService.rotateSession(refreshToken, tokens.jwt(), tokens.jwtRefresh())
                                .thenReturn(tokens))
                        .doOnSuccess(tokens -> log.info("Refresh successful user={}", user.getUsername())));
    }

    @Override
    public Mono<AuthToken> buildTokensForUser(Usuario user, String algorithm) {
        return buildAuthToken(user, normalizeAlgorithm(algorithm));
    }

    @Override
    public Mono<AuthToken> logout(LogoutRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Body requerido"));
        }
        if (!StringUtils.hasText(request.accessToken())) {
            return Mono.error(new IllegalArgumentException("accessToken es requerido"));
        }
        if (!jwtService.isValid(request.accessToken())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access token inválido o expirado"));
        }
        return jwtTokenRevocationService.markSessionUnavailable(request.accessToken(), request.refreshToken())
                .thenReturn(AuthToken.loggedOut());
    }

    @Override
    public Mono<ValidateTokenResponse> validateToken(String token) {
        log.debug("Validate token endpoint called");
        if (token == null || token.isBlank()) {
            return Mono.just(ValidateTokenResponse.invalid());
        }
        if (!jwtService.isValid(token)) {
            return Mono.just(ValidateTokenResponse.invalid());
        }
        Map<String, Object> header = jwtService.getHeader(token);
        JwtClaims claims = jwtService.parse(token);
        Map<String, Object> payload = claimsToMap(claims);
        return Mono.just(new ValidateTokenResponse(true, header, payload));
    }

    @Override
    public Mono<UserProfileResponse> getCurrentUserProfile() {
        log.debug("Fetching current user profile from reactive security context");
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(JwtManagerImpl::authenticationToUsuario)
                .map(JwtManagerImpl::toUserProfileResponse)
                .doOnNext(p -> log.info("Perfil entregado user={} role={} operationCount={}",
                        p.username(), p.roleName(), p.operationNames().size()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado")));
    }

    private static Mono<Usuario> authenticationToUsuario(SecurityContext ctx) {
        Authentication auth = ctx.getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Mono.empty();
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            return Mono.empty();
        }
        return Mono.just((Usuario) principal);
    }

    private static UserProfileResponse toUserProfileResponse(Usuario u) {
        String roleName = u.getRol() != null ? u.getRol().getName() : null;
        return new UserProfileResponse(
                u.getUsername(),
                u.getEmail(),
                u.getPhone(),
                roleName,
                UserOperationNames.fromUsuario(u)
        );
    }

    private static Map<String, Object> claimsToMap(JwtClaims c) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (c == null) return m;
        if (c.getSub() != null) m.put("sub", c.getSub());
        if (c.getIat() != null) m.put("iat", c.getIat());
        if (c.getExp() != null) m.put("exp", c.getExp());
        if (c.getIss() != null) m.put("iss", c.getIss());
        if (c.getAud() != null) m.put("aud", c.getAud());
        if (c.getClaims() != null) m.putAll(c.getClaims());
        return m;
    }

    private Mono<Usuario> enrichUserWithRole(Usuario user) {
        log.debug("Loading role for userId={}", user.getId());
        return userRolRepositoryPort.findRoleByUserId(user.getId())
                .doOnNext(user::setRol)
                .thenReturn(user)
                .defaultIfEmpty(user);
    }

    /** Carga operaciones del rol en BD vía {@link ReactiveUserAuthoritiesLoader}. */
    private Mono<Usuario> enrichUserWithAuthorities(Usuario user) {
        return ReactiveUserAuthoritiesLoader.loadAuthoritiesFromDb(rolOperationRepositoryPort, user);
    }

    private Mono<AuthToken> buildAuthToken(Usuario user, String algorithm) {
        String access = jwtService.generateToken(buildJwt(user, accessExpirationSeconds, SecurityConstants.JWT_HEADER_TYP_ACCESS, algorithm));
        String refresh = jwtService.generateToken(buildJwt(user, refreshExpirationSeconds, SecurityConstants.JWT_HEADER_TYP_REFRESH, algorithm));
        AuthToken tokens = new AuthToken(access, refresh, true);
        return jwtTokenRevocationService.registerIssuedPair(access, refresh).thenReturn(tokens);
    }

    private Map<String, Object> generateExtraClaims(Usuario user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("name", user.getUsername());
        extraClaims.put("role", user.getRol() != null ? user.getRol().getName() : null);
        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        extraClaims.put("authorities", authorities);
        return extraClaims;
    }

    private static String normalizeAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            return SecurityConstants.DEFAULT_JWT_ALGORITHM;
        }
        return algorithm.trim().toUpperCase();
    }

    private Jwt buildJwt(Usuario user, long expirationSeconds, String type, String algorithm) {
        String alg = normalizeAlgorithm(algorithm);
        JwtHeader header = new JwtHeader();
        header.setAlg(alg);
        header.setTyp(type);

        JwtPayload payload = new JwtPayload();
        payload.setSub(user.getUsername());
        payload.setIat(Instant.now().getEpochSecond());
        payload.setExp(Instant.now().getEpochSecond() + expirationSeconds);
        payload.setIss("findu-spring-security");
        payload.setClaims(generateExtraClaims(user));

        Jwt jwt = new Jwt();
        jwt.setHeader(header);
        jwt.setPayload(payload);
        jwt.setSigner(jwtSignerFactory.getSigner(alg));
        return jwt;
    }
}
