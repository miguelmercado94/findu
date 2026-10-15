package com.findu.notification.processor.application.port.out;

import com.findu.notification.processor.domain.model.EventEnvelope;

public interface AlertEventPublisherPort {
    void publish(EventEnvelope<?> envelope);
}
