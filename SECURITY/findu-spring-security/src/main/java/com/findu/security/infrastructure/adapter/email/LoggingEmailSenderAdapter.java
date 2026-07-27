package com.findu.security.infrastructure.adapter.email;

import com.findu.security.application.port.output.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementación stub del envío de correo: solo registra en log.
 * Sustituir por un adapter real (SMTP, SendGrid, etc.) en producción.
 */
@Component
public class LoggingEmailSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSenderAdapter.class);

    @Override
    public Mono<Void> sendPasswordRecoveryEmail(String toEmail, String resetLinkUrl) {
        return Mono.fromRunnable(() ->
                log.info("Password recovery email (stub): to={}, resetLink={}", toEmail, resetLinkUrl))
                .then();
    }

    @Override
    public Mono<Void> sendPasswordRecoveryCode(String toEmail, String code) {
        return Mono.fromRunnable(() ->
                log.info("Password recovery code email (stub): to={}, code={}", toEmail, code))
                .then();
    }
}
