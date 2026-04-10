package com.findu.security.messages;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.findu.security.domain.model.Usuario;

/**
 * Textos de API centralizados (errores, seguridad, validaciones comunes).
 */
public final class ApiMessages {

    private ApiMessages() {
    }

    public static final class Security {
        public static final String UNAUTHENTICATED_OR_INVALID_TOKEN = "No autenticado o token inválido.";
        public static final String ACCESS_DENIED_FALLBACK = "No tienes permiso para acceder a este recurso.";
        public static final String ACCESS_DENIED_ANONYMOUS = "No hay una sesión válida con permiso para %s %s. Inicia sesión o solicita acceso al administrador.";
        public static final String ACCESS_DENIED_WITH_USER = "Tu usuario \"%s\" (rol %s) no tiene permiso para %s %s. Si necesitas este acceso, contacta al administrador.";
        public static final String ACCESS_DENIED_WITHOUT_ROLE = "Tu usuario \"%s\" no tiene permiso para %s %s. Si necesitas este acceso, contacta al administrador.";

        private Security() {
        }

        public static String accessDeniedUserMessage(Authentication auth, String method, String path) {
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return String.format(ACCESS_DENIED_ANONYMOUS, method, path);
            }
            String username = extractUsername(auth);
            String role = extractRole(auth);
            if (role != null && !role.isBlank()) {
                return String.format(ACCESS_DENIED_WITH_USER, username, role, method, path);
            }
            return String.format(ACCESS_DENIED_WITHOUT_ROLE, username, method, path);
        }

        private static String extractUsername(Authentication auth) {
            Object p = auth.getPrincipal();
            if (p instanceof Usuario u) {
                return u.getUsername() != null ? u.getUsername() : "(desconocido)";
            }
            if (p instanceof String s && !s.isBlank()) {
                return s;
            }
            return auth.getName() != null ? auth.getName() : "(desconocido)";
        }

        private static String extractRole(Authentication auth) {
            Object p = auth.getPrincipal();
            if (p instanceof Usuario u && u.getRol() != null && u.getRol().getName() != null) {
                return u.getRol().getName();
            }
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a != null && a.startsWith("ROLE_"))
                    .findFirst()
                    .orElse("");
        }
    }

    public static final class Error {
        public static final String INTERNAL = "Error interno. Intenta de nuevo más tarde.";

        private Error() {
        }
    }
}
