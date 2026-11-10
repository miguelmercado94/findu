package com.findu.core.infrastructure.security;

import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifica que el JWT pertenezca al proveedor dueno del recurso. Se invoca ANTES de buscar el recurso.
 * OFF: no hace nada. LOG: registra AUTHZ_VIOLATION y deja pasar. ENFORCE: 401 (sin token / token invalido)
 * o 403 (otro proveedor) si el ambito esta en enforce-scopes (lista vacia = todos).
 * Un request con X-Internal-Service-Key valida (marcado por el filtro) cuenta como servicio-a-servicio.
 */
@Component
public class ProviderOwnershipGuard {

    private static final Logger log = LoggerFactory.getLogger(ProviderOwnershipGuard.class);

    private final AuthzProperties props;
    private final PerfilProveedorRepository perfilRepository;
    private final HttpServletRequest request;

    public ProviderOwnershipGuard(AuthzProperties props, PerfilProveedorRepository perfilRepository,
                                  HttpServletRequest request) {
        this.props = props;
        this.perfilRepository = perfilRepository;
        this.request = request;
    }

    /** El authUserId ya viene en la ruta: se compara directo con el claim userId (sin BD). */
    public void requireAuthUser(String scope, Long authUserId) {
        if (skip()) {
            return;
        }
        AuthenticatedPrincipal p = principal();
        if (p == null) {
            violation(scope, noTokenReason(), HttpStatus.UNAUTHORIZED, null);
        } else if (authUserId == null || !authUserId.equals(p.userId())) {
            violation(scope, "NOT_OWNER", HttpStatus.FORBIDDEN, p);
        }
    }

    /** Resuelve perfil_proveedor.auth_user_id por BD. Perfil inexistente: pasa (el 404 lo da el recurso). */
    public void requirePerfil(String scope, Long perfilId) {
        if (skip()) {
            return;
        }
        AuthenticatedPrincipal p = principal();
        if (p == null) {
            violation(scope, noTokenReason(), HttpStatus.UNAUTHORIZED, null);
            return;
        }
        if (perfilId == null) {
            return;
        }
        perfilRepository.findById(perfilId).ifPresent(perfil -> {
            if (!p.userId().equals(perfil.getAuthUserId())) {
                violation(scope, "NOT_OWNER", HttpStatus.FORBIDDEN, p);
            }
        });
    }

    private boolean skip() {
        return props.getMode() == AuthzMode.OFF
                || Boolean.TRUE.equals(request.getAttribute(JwtContextFilter.ATTR_INTERNAL_OK));
    }

    private AuthenticatedPrincipal principal() {
        Object p = request.getAttribute(JwtContextFilter.ATTR_PRINCIPAL);
        return p instanceof AuthenticatedPrincipal ap ? ap : null;
    }

    private String noTokenReason() {
        return JwtContextFilter.BAD_TOKEN.equals(request.getAttribute(JwtContextFilter.ATTR_TOKEN_STATE))
                ? JwtContextFilter.BAD_TOKEN : JwtContextFilter.NO_TOKEN;
    }

    private void violation(String scope, String reason, HttpStatus status, AuthenticatedPrincipal p) {
        boolean enforced = props.isScopeEnforced(scope);
        log.warn("AUTHZ_VIOLATION reason={} scope={} mode={} enforced={} method={} path={} userId={}",
                reason, scope, props.getMode(), enforced, request.getMethod(), request.getRequestURI(),
                p != null ? p.userId() : null);
        if (enforced) {
            throw new ResponseStatusException(status,
                    status == HttpStatus.UNAUTHORIZED ? "Autenticacion requerida" : "Acceso denegado");
        }
    }
}
