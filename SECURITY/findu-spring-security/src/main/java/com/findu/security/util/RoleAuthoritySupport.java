package com.findu.security.util;

import com.findu.security.domain.model.Operation;
import com.findu.security.domain.model.Rol;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Construcción de {@link GrantedAuthority} en memoria: operaciones desde BD + una authority de rol
 * con prefijo {@code ROLE_} (no persistida como fila en {@code operation}).
 */
public final class RoleAuthoritySupport {

    private RoleAuthoritySupport() {
    }

    /**
     * Authorities desde operaciones ({@code operation.name}) más la authority del rol
     * {@code ROLE_}{@code rol.getName()} si aplica (si el nombre ya empieza por {@code ROLE_}, no se duplica).
     */
    public static List<GrantedAuthority> fromOperationsAndRole(List<Operation> operations, Rol rol) {
        List<GrantedAuthority> list = new ArrayList<>();
        if (operations != null && !operations.isEmpty()) {
            list.addAll(operations.stream()
                    .map(Operation::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()));
        }
        appendRoleAuthority(list, rol);
        return list;
    }

    /**
     * Añade la authority de rol solo en el modelo cargado (no existe como fila en {@code operation}).
     */
    public static void appendRoleAuthority(List<GrantedAuthority> authorities, Rol rol) {
        if (authorities == null || rol == null) {
            return;
        }
        String name = rol.getName();
        if (name == null || name.isBlank()) {
            return;
        }
        String n = name.trim();
        String value = n.startsWith("ROLE_") ? n : "ROLE_" + n;
        authorities.add(new SimpleGrantedAuthority(value));
    }
}
