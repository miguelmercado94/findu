package com.findu.security.messages;

import com.findu.security.domain.model.Rol;
import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiMessagesSecurityTest {

    @Test
    void accessDeniedUserMessage_includesUserAndRole_forUsuarioPrincipal() {
        Usuario u = new Usuario();
        u.setUsername("juan");
        Rol r = new Rol();
        r.setName("ROLE_CUSTOMER");
        u.setRol(r);
        u.setGrantedAuthorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        var auth = new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
        String msg = ApiMessages.Security.accessDeniedUserMessage(auth, "GET", "/api/v1/customers");

        assertThat(msg).contains("juan").contains("ROLE_CUSTOMER").contains("GET").contains("/api/v1/customers");
    }

    @Test
    void accessDeniedUserMessage_anonymous_usesTemplateWithoutUser() {
        var auth = new AnonymousAuthenticationToken("key", "anon", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        String msg = ApiMessages.Security.accessDeniedUserMessage(auth, "GET", "/x");
        assertThat(msg).contains("GET").contains("/x");
    }
}
