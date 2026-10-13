package com.findu.help.v2.infrastructure.adapter.out.event;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.event.TicketCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class HttpAsyncEventPublisherTest {

    @Test
    @DisplayName("Debe manejar evento asíncrono sin lanzar excepciones incluso si el dispatcher no responde")
    void publishAsync_HandledSafely() {
        HttpAsyncEventPublisher publisher = new HttpAsyncEventPublisher("http://localhost:9999");
        TicketCreatedEvent event = new TicketCreatedEvent("t-1", "HT-001", 100L, UserRole.CLIENTE, 500L, PriorityLevel.ALTA);

        assertThatNoException().isThrownBy(() -> publisher.publishAsync(event));
    }
}
