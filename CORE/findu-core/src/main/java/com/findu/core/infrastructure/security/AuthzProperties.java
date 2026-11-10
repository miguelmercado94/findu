package com.findu.core.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Propiedades {@code findu.security.authz.*}.
 * <ul>
 *   <li>mode: OFF (default) | LOG | ENFORCE.</li>
 *   <li>enforceScopes: ambitos que se bloquean en ENFORCE (p.ej. archivos,perfil). Vacio = todos los ambitos
 *       instrumentados. Los ambitos fuera de la lista solo se registran (como LOG).</li>
 *   <li>jwtSecret: mismo valor que JWT_SECRET de findu-spring-security (Base64 o UTF-8).</li>
 *   <li>internalServiceKey: llave S2S (X-Internal-Service-Key).</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "findu.security.authz")
public class AuthzProperties {

    private AuthzMode mode = AuthzMode.OFF;
    private List<String> enforceScopes = new ArrayList<>();
    private String jwtSecret = "";
    private String internalServiceKey = "";

    /** true si el ambito debe bloquearse (modo ENFORCE y ambito incluido, o lista vacia). */
    public boolean isScopeEnforced(String scope) {
        if (mode != AuthzMode.ENFORCE) {
            return false;
        }
        if (enforceScopes == null || enforceScopes.isEmpty()) {
            return true;
        }
        return enforceScopes.stream().anyMatch(s -> s != null && s.trim().equalsIgnoreCase(scope));
    }
}
