package com.findu.help.v2.domain.valueobject;

import com.findu.help.v2.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketRequesterTest {

    @Test
    @DisplayName("Debe instanciar correctamente un TicketRequester validado")
    void create_Success() {
        TicketRequester requester = new TicketRequester(10L, UserRole.CLIENTE, "Carlos Gomez", "carlos@example.com");

        assertThat(requester.getUserId()).isEqualTo(10L);
        assertThat(requester.getUserRole()).isEqualTo(UserRole.CLIENTE);
        assertThat(requester.getName()).isEqualTo("Carlos Gomez");
        assertThat(requester.getEmail()).isEqualTo("carlos@example.com");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando falta userId")
    void create_MissingUserId_ThrowsException() {
        assertThatThrownBy(() -> new TicketRequester(null, UserRole.CLIENTE, "Carlos", "carlos@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
