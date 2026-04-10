package com.findu.security.util;

import com.findu.security.domain.model.Rol;
import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserOperationNamesTest {

    @Test
    void fromUsuario_excludesRoleAuthorities_and_sorts() {
        Usuario u = new Usuario();
        u.setGrantedAuthorities(List.of(
                new SimpleGrantedAuthority("Z_OP"),
                new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                new SimpleGrantedAuthority("A_OP")
        ));
        assertThat(UserOperationNames.fromUsuario(u)).containsExactly("A_OP", "Z_OP");
    }

    @Test
    void fromUsuario_nullOrEmptyAuthorities_returnsEmpty() {
        Usuario u = new Usuario();
        assertThat(UserOperationNames.fromUsuario(u)).isEmpty();
        u.setGrantedAuthorities(List.of());
        assertThat(UserOperationNames.fromUsuario(u)).isEmpty();
    }

    @Test
    void fromUsuario_includesRoleFromModel_onlyAsAuthorityNotInOperationList() {
        Usuario u = new Usuario();
        Rol rol = new Rol();
        rol.setName("ROLE_CUSTOMER");
        u.setRol(rol);
        u.setGrantedAuthorities(RoleAuthoritySupport.fromOperationsAndRole(
                List.of(), rol));
        assertThat(UserOperationNames.fromUsuario(u)).isEmpty();
    }
}
