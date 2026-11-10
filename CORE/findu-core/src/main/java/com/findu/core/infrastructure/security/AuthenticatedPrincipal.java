package com.findu.core.infrastructure.security;

/** Identidad extraida de un JWT valido: userId = auth_user_id, username = sub, role = claim role. */
public record AuthenticatedPrincipal(Long userId, String username, String role) {
}
