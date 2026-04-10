package com.findu.security.application.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

class HealthApplicationServiceImplTest {

    @Test
    void getHealth_returnsUpAndApplicationName() {
        HealthApplicationServiceImpl svc = new HealthApplicationServiceImpl();
        ReflectionTestUtils.setField(svc, "applicationName", "my-app");

        StepVerifier.create(svc.getHealth())
                .expectNextMatches(m -> "UP".equals(m.get("status")) && "my-app".equals(m.get("application")))
                .verifyComplete();
    }
}
