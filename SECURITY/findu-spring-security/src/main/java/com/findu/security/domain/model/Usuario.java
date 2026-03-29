package com.findu.security.domain.model;

import com.findu.security.util.SecurityConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modelo de dominio Usuario.
 * Implementa UserDetails para integrar con Spring Security.
 * Rol no está persistido en tabla user; se carga desde user_rol + role.
 * getAuthorities() devuelve las authorities cargadas (rol → operaciones en BD o por defecto); si no se han seteado, usa operaciones por defecto.
 */
@Getter
@Setter
public class Usuario implements UserDetails {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String password;
    private boolean active = true;
    /** Rol del usuario (para JWT/extraClaims; no persistido en tabla user). */
    private Rol rol;

    /** Authorities derivadas del rol (operaciones en BD o por defecto). Seteadas por UsuarioService al cargar usuario con rol. */
    private List<GrantedAuthority> grantedAuthorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (grantedAuthorities != null && !grantedAuthorities.isEmpty()) {
            return grantedAuthorities;
        }
        return SecurityConstants.DEFAULT_OPERATION_NAMES.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
