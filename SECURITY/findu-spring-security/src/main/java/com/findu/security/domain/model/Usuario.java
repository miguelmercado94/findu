package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Usuario de dominio. Implementa {@link UserDetails}.
 * El rol y las authorities no vienen solo de la fila {@code user}; se cargan en memoria (user_rol, rol_operation, etc.).
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
    private Rol rol;
    private List<GrantedAuthority> grantedAuthorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (grantedAuthorities == null || grantedAuthorities.isEmpty()) {
            return Collections.emptyList();
        }
        return grantedAuthorities;
    }

    @Override
    public boolean isEnabled() {
        return active;
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
}
