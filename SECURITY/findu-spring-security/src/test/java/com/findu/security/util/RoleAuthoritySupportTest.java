package com.findu.security.util;

import com.findu.security.domain.model.Operation;
import com.findu.security.domain.model.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAuthoritySupportTest {

    @Test
    void fromOperationsAndRole_addsOperationAuthorities_andRole() {
        Operation op = new Operation();
        op.setName("CUST_LIST");
        Rol rol = new Rol();
        rol.setName("ROLE_CUSTOMER");

        List<GrantedAuthority> list = RoleAuthoritySupport.fromOperationsAndRole(List.of(op), rol);

        assertThat(list).extracting(GrantedAuthority::getAuthority)
                .contains("CUST_LIST", "ROLE_CUSTOMER");
    }

    @Test
    void appendRoleAuthority_doesNotDuplicateRolePrefix() {
        Rol rol = new Rol();
        rol.setName("ROLE_ADMIN");
        List<GrantedAuthority> list = new java.util.ArrayList<>();
        RoleAuthoritySupport.appendRoleAuthority(list, rol);
        assertThat(list).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }
}
