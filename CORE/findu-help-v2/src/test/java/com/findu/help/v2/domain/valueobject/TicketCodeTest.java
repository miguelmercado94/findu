package com.findu.help.v2.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketCodeTest {

    @Test
    @DisplayName("Debe generar un TicketCode con el prefijo indicado")
    void generate_Success() {
        TicketCode code = TicketCode.generate("HT");
        assertThat(code).isNotNull();
        assertThat(code.getValue()).startsWith("HT-");
    }

    @Test
    @DisplayName("Debe crear un TicketCode desde un valor existente")
    void of_Success() {
        TicketCode code = new TicketCode("HT-20260928-12345");
        assertThat(code.getValue()).isEqualTo("HT-20260928-12345");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el valor es nulo o vacío")
    void of_NullOrBlank_ThrowsException() {
        assertThatThrownBy(() -> new TicketCode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TicketCode("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
