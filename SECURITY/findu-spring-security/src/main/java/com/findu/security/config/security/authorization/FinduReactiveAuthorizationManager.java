package com.findu.security.config.security.authorization;

import com.findu.security.application.port.output.persistence.OperationRepositoryPort;
import com.findu.security.domain.model.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Autorización por operación en BD: URI = {@code spring.webflux.base-path} (módulo) + {@code operation.path}, método HTTP.
 * <ul>
 *   <li>Sin autenticación real: solo se permite si la operación existe, está activa y {@code permite_all = true} (si no → denegado / 401).</li>
 *   <li>Con JWT/usuario autenticado: la operación debe existir y estar activa; el usuario debe tener la authority {@code operation.name}
 *       alineada con la fila en BD para esa URI+método.</li>
 *   <li>Si no hay operación para esa URI+módulo+método → denegado (403).</li>
 * </ul>
 */
@Component
public class FinduReactiveAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private static final Logger log = LoggerFactory.getLogger(FinduReactiveAuthorizationManager.class);

    private final OperationRepositoryPort operationRepositoryPort;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    private final String webFluxBasePath;

    public FinduReactiveAuthorizationManager(OperationRepositoryPort operationRepositoryPort,
                                            @Value("${spring.webflux.base-path:}") String webFluxBasePath) {
        this.operationRepositoryPort = operationRepositoryPort;
        this.webFluxBasePath = webFluxBasePath;
    }

    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentication, AuthorizationContext context) {
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
                            return authorizeAuthenticated(auth, operation);
                        })
                        .switchIfEmpty(Mono.defer(() -> authorizeAnonymous(operation)))
                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Denegado: sin operación en BD para modulePathBase={} path={} method={}", modulePathBase, apiPath, method);
                    return Mono.just(new AuthorizationDecision(false));
                }));
    }

    private Mono<Operation> resolveOperation(String modulePathBase, String apiPath, String httpMethod) {
        if (modulePathBase == null || modulePathBase.isEmpty()) {
            return Mono.empty();
        }
        return operationRepositoryPort.findByModulePathBaseAndPathAndHttpMethod(modulePathBase, apiPath, httpMethod);
    }

    /** Invitado / sin token en contexto: solo {@code permite_all}. */
    private Mono<AuthorizationResult> authorizeAnonymous(Operation operation) {
        if (!operation.isActive() || !operation.isPermiteAll()) {
            log.debug("Denegado anónimo: op={} active={} permiteAll={}", operation.getName(), operation.isActive(), operation.isPermiteAll());
            return Mono.just(new AuthorizationDecision(false));
        }
        log.trace("Permitido anónimo: op={}", operation.getName());
        return Mono.just(new AuthorizationDecision(true));
    }

    /** Usuario autenticado: debe poseer la authority {@code operation.name} para esa operación en BD. */
    private Mono<AuthorizationResult> authorizeAuthenticated(Authentication auth, Operation operation) {
        if (!operation.isActive()) {
            log.debug("Denegado: operación inactiva name={}", operation.getName());
            return Mono.just(new AuthorizationDecision(false));
        }
        String required = operation.getName();
        if (required == null || required.isBlank()) {
            log.debug("Denegado: operación sin name en BD");
            return Mono.just(new AuthorizationDecision(false));
        }
        boolean hasOp = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(required::equals);
        if (!hasOp) {
            log.debug("Denegado autenticado: requiere authority={} principal={} authorities={}",
                    required, auth.getName(), auth.getAuthorities());
        } else {
            log.trace("Permitido autenticado: authority={} principal={}", required, auth.getName());
        }
        return Mono.just(new AuthorizationDecision(hasOp));
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
