package com.findu.security.util;

import com.findu.security.domain.model.Usuario;
import org.springframework.security.core.GrantedAuthority;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Lista de nombres de operación ({@code operation.name}) a partir del usuario en contexto,
 * excluyendo authorities de rol ({@code ROLE_*}).
 */
public final class UserOperationNames {

    private UserOperationNames() {
    }

    public static List<String> fromUsuario(Usuario u) {
        if (u == null || u.getAuthorities() == null) {
            return List.of();
        }
        return u.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(a -> !a.isBlank())
                .filter(a -> !a.startsWith("ROLE_"))
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
