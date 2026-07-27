package com.findu.security.config.security.authorization;

import com.findu.security.application.port.output.persistence.OperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Autorización por operación en BD: URI = {@code spring.webflux.base-path} (módulo) + {@code operation.path}, método HTTP.
 * <ul>
 *   <li>Sin autenticación real: solo se permite si la operación existe, está activa y {@code permite_all = true} (si no → denegado / 401).</li>
 *   <li>Con JWT/usuario autenticado: se consultan dinámicamente sus operaciones de base de datos para validar los permisos en tiempo real.</li>
 * </ul>
 */
@Component
public class FinduReactiveAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private static final Logger log = LoggerFactory.getLogger(FinduReactiveAuthorizationManager.class);

    private final OperationRepositoryPort operationRepositoryPort;
    private final UsuarioService usuarioService;
    private final UserRolRepositoryPort userRolRepositoryPort;
    private final RolOperationRepositoryPort rolOperationRepositoryPort;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    private final String webFluxBasePath;

    public FinduReactiveAuthorizationManager(OperationRepositoryPort operationRepositoryPort,
                                             UsuarioService usuarioService,
                                             UserRolRepositoryPort userRolRepositoryPort,
                                             RolOperationRepositoryPort rolOperationRepositoryPort,
                                             @Value("${spring.webflux.base-path:}") String webFluxBasePath) {
        this.operationRepositoryPort = operationRepositoryPort;
        this.usuarioService = usuarioService;
        this.userRolRepositoryPort = userRolRepositoryPort;
        this.rolOperationRepositoryPort = rolOperationRepositoryPort;
        this.webFluxBasePath = webFluxBasePath;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        ServerWebExchange exchange = context.getExchange();
        String apiPath = normalizeApiPath(exchange.getRequest().getPath().pathWithinApplication().value());
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "";
        String modulePathBase = normalizePathSegment(webFluxBasePath);

        log.trace("Autorización: modulePathBase={} path={} method={}", modulePathBase, apiPath, method);

        return resolveOperation(modulePathBase, apiPath, method)
                .doOnNext(op -> log.trace("Operación BD: name={} permiteAll={} path={} {}", op.getName(), op.isPermiteAll(), apiPath, method))
                .flatMap(operation -> authentication
                        .flatMap(auth -> {
                            if (trustResolver.isAnonymous(auth)) {
                                return authorizeAnonymous(operation);
                            }
                            return obtainOperations(auth)
                                    .map(userOperations -> {
                                        boolean hasOp = userOperations.contains(operation.getName());
                                        if (!hasOp) {
                                            log.debug("Denegado autenticado: requiere authority={} principal={} operations={}",
                                                    operation.getName(), auth.getName(), userOperations);
                                        } else {
                                            log.trace("Permitido autenticado: authority={} principal={}", operation.getName(), auth.getName());
                                        }
                                        return new AuthorizationDecision(hasOp);
                                    });
                        })
                        .switchIfEmpty(Mono.defer(() -> authorizeAnonymous(operation)))
                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Denegado: sin operación en BD para modulePathBase={} path={} method={}", modulePathBase, apiPath, method);
                    return Mono.just(new AuthorizationDecision(false));
                }));
    }

    private Mono<List<String>> obtainOperations(Authentication auth) {
        String username = auth.getName();
        if (username == null || username.isBlank()) {
            return Mono.just(java.util.Collections.emptyList());
        }

        Mono<List<String>> scopesMono = Mono.just(java.util.Collections.emptyList());
        if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
            org.springframework.security.oauth2.jwt.Jwt jwt = jwtAuth.getToken();
            scopesMono = Mono.just(extractScopes(jwt));
        }

        return scopesMono.flatMap(scopes -> usuarioService.getUserByUsername(username)
                .switchIfEmpty(Mono.defer(() -> usuarioService.getUserByEmail(username)))
                .flatMap(user -> userRolRepositoryPort.findRoleByUserId(user.getId())
                        .flatMap(rol -> rolOperationRepositoryPort.findOperationsByRoleId(rol.getId())
                                .map(com.findu.security.domain.model.Operation::getName)
                                .collectList()
                        )
                )
                .map(userOperations -> {
                    if (!scopes.contains("ALL")) {
                        return userOperations.stream()
                                .filter(scopes::contains)
                                .collect(java.util.stream.Collectors.toList());
                    }
                    return userOperations;
                })
        ).defaultIfEmpty(java.util.Collections.emptyList());
    }

    private List<String> extractScopes(org.springframework.security.oauth2.jwt.Jwt jwt) {
        try {
            Object scopeClaim = jwt.getClaims().get("scope");
            if (scopeClaim == null) {
                // Si no hay scope claim (token directo de usuario local), permitimos todo (ALL)
                return java.util.List.of("ALL");
            }
            if (scopeClaim instanceof java.util.Collection) {
                return ((java.util.Collection<?>) scopeClaim).stream()
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.toList());
            }
            if (scopeClaim instanceof String StringScope) {
                if (StringScope.trim().isEmpty()) {
                    return java.util.List.of("ALL");
                }
                return java.util.Arrays.asList(StringScope.split(" "));
            }
        } catch (Exception exception) {
            log.warn("Hubo un problema al extraer los scopes del cliente");
        }
        return java.util.Collections.emptyList();
    }

    private Mono<Operation> resolveOperation(String modulePathBase, String apiPath, String httpMethod) {
        if (modulePathBase == null || modulePathBase.isEmpty()) {
            return Mono.empty();
        }
        return operationRepositoryPort.findByModulePathBaseAndPathAndHttpMethod(modulePathBase, apiPath, httpMethod);
    }

    private Mono<AuthorizationDecision> authorizeAnonymous(Operation operation) {
        if (!operation.isActive() || !operation.isPermiteAll()) {
            log.debug("Denegado anónimo: op={} active={} permiteAll={}", operation.getName(), operation.isActive(), operation.isPermiteAll());
            return Mono.just(new AuthorizationDecision(false));
        }
        log.trace("Permitido anónimo: op={}", operation.getName());
        return Mono.just(new AuthorizationDecision(true));
    }

    private static String normalizeApiPath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String p = path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p;
    }

    /** Alinea con {@code module.path_base} (ej. {@code security-auth} sin barras). */
    private static String normalizePathSegment(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            return "";
        }
        String s = basePath.trim();
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        while (s.endsWith("/") && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
