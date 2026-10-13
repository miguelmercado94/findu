package com.findu.help.v2.infrastructure.adapter.out.event;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.event.TicketCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class HttpRealtimeEventPublisherTest {

    @Test
    @DisplayName("Debe manejar evento en tiempo real sin lanzar excepciones incluso si la red no está disponible")
    void publishRealtime_HandledSafely() {
        HttpRealtimeEventPublisher publisher = new HttpRealtimeEventPublisher("http://localhost:9999");
        TicketCreatedEvent event = new TicketCreatedEvent("t-1", "HT-001", 100L, UserRole.CLIENTE, 500L, PriorityLevel.ALTA);

        assertThatNoException().isThrownBy(() -> publisher.publishRealtime(event));
    }
}
