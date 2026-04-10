package com.findu.security.infrastructure.adapter.email;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class LoggingEmailSenderAdapterTest {

    @Test
    void sendPasswordRecoveryEmail_completes() {
        LoggingEmailSenderAdapter a = new LoggingEmailSenderAdapter();
        StepVerifier.create(a.sendPasswordRecoveryEmail("a@b.com", "http://link"))
                .verifyComplete();
    }
}
