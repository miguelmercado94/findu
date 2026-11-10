package com.findu.dispatcher.infrastructure.security;

import com.findu.dispatcher.infrastructure.routing.RecipientResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Autoriza el handshake del WebSocket segun {@code findu.websocket.auth.mode}:
 * <ul>
 *   <li>OFF: comportamiento historico (acepta ?userId= sin validar token).</li>
 *   <li>LOG: acepta como antes pero registra {@code WS_AUTH_VIOLATION reason=...} (nunca el token).</li>
 *   <li>ENFORCE: sin token valido o con canal ajeno el handshake se rechaza (401/403); la identidad sale
 *       del token y el userId del query solo se compara con el canal derivado del token.</li>
 * </ul>
 * El canal canonico PROVEEDOR_&lt;id&gt; debe pertenecer al claim userId; core resuelve authUserId -&gt; proveedorId.
 * Canales que no son de proveedor (CLIENTE_, usernames...) no se pueden verificar contra ese endpoint:
 * ENFORCE los rechaza (403 CHANNEL_MISMATCH); LOG los acepta y registra la violacion.
 */
@Slf4j
@Component
public class WebSocketAuthService {

    public enum Mode { OFF, LOG, ENFORCE }

    /**
     * @param accepted si el handshake continua
     * @param channel  clave de canal (sin canonizar si viene del query en OFF; canonica en LOG/ENFORCE)
     * @param httpStatus 401/403 cuando se rechaza antes de registrar la sesion (0 si se acepta)
     * @param reason   NO_TOKEN | BAD_TOKEN | CHANNEL_MISMATCH | CORE_UNAVAILABLE | NO_IDENTITY (null si se acepta limpio)
     */
    public record Decision(boolean accepted, String channel, int httpStatus, String reason) {
        static Decision accept(String channel) {
            return new Decision(true, channel, 0, null);
        }

        static Decision reject(int status, String reason) {
            return new Decision(false, null, status, reason);
        }
    }

    private static final String PREFIJO_PROVEEDOR = "PROVEEDOR_";

    private final JwtValidator jwtValidator;
    private final CoreProfileClient coreClient;
    private final RecipientResolver recipientResolver;
    private final Mode mode;

    public WebSocketAuthService(JwtValidator jwtValidator, CoreProfileClient coreClient,
                                RecipientResolver recipientResolver,
                                @Value("${findu.websocket.auth.mode:OFF}") String mode) {
        this.jwtValidator = jwtValidator;
        this.coreClient = coreClient;
        this.recipientResolver = recipientResolver;
        this.mode = parseMode(mode);
        log.info("Autenticacion del WebSocket: findu.websocket.auth.mode={}", this.mode);
    }

    private static Mode parseMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Mode.OFF;
        }
        try {
            return Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("findu.websocket.auth.mode='{}' invalido; se usa OFF", raw);
            return Mode.OFF;
        }
    }

    public Mode mode() {
        return mode;
    }

    public Mono<Decision> authorize(URI uri) {
        String requested = queryParam(uri, "userId");
        String token = queryParam(uri, "token");

        if (mode == Mode.OFF) {
            return Mono.just(authorizeLegacy(requested, token));
        }

        String requestedCanonical = requested == null ? null : recipientResolver.toCanonical(requested);

        if (token == null) {
            return violation("NO_TOKEN", null, requested, 401, requestedCanonical);
        }
        Optional<String> authUserId = jwtValidator.validateAccessToken(token);
        if (authUserId.isEmpty()) {
            return violation("BAD_TOKEN", null, requested, 401, requestedCanonical);
        }
        String userId = authUserId.get();

        // Canal solicitado que no es de proveedor: no es verificable contra core, asi que no se puede probar que
        // pertenece al token. ENFORCE lo rechaza (403); LOG lo acepta y registra la violacion.
        if (requestedCanonical != null && !requestedCanonical.startsWith(PREFIJO_PROVEEDOR)) {
            return Mono.just(violationNow("CHANNEL_MISMATCH", userId, requested, 403, requestedCanonical));
        }

        return coreClient.resolveProveedorId(userId)
                .map(opt -> {
                    if (opt.isEmpty()) {
                        return violationNow("CHANNEL_MISMATCH", userId, requested, 403, requestedCanonical);
                    }
                    String own = PREFIJO_PROVEEDOR + opt.get();
                    if (requestedCanonical != null && !requestedCanonical.equals(own)) {
                        return violationNow("CHANNEL_MISMATCH", userId, requested, 403, requestedCanonical);
                    }
                    return Decision.accept(own);
                })
                .onErrorResume(CoreProfileClient.CoreUnavailableException.class,
                        e -> Mono.just(violationNow("CORE_UNAVAILABLE", userId, requested, 403, requestedCanonical)));
    }

    private Decision authorizeLegacy(String requested, String token) {
        if (requested != null) {
            return Decision.accept(requested);
        }
        if (token == null) {
            return Decision.reject(400, "NO_IDENTITY");
        }
        return jwtValidator.validateAccessToken(token)
                .map(Decision::accept)
                .orElseGet(() -> Decision.reject(401, "BAD_TOKEN"));
    }

    private Mono<Decision> violation(String reason, String userId, String requested, int enforceStatus, String requestedCanonical) {
        return Mono.just(violationNow(reason, userId, requested, enforceStatus, requestedCanonical));
    }

    /** LOG: acepta con el canal solicitado (si no hay ninguno no se puede identificar y se rechaza). ENFORCE: rechaza. */
    private Decision violationNow(String reason, String userId, String requested, int enforceStatus, String requestedCanonical) {
        log.warn("WS_AUTH_VIOLATION reason={} userId={} requested={} mode={}",
                reason, safe(userId), safe(requested), mode);
        if (mode == Mode.ENFORCE) {
            return Decision.reject(enforceStatus, reason);
        }
        if (requestedCanonical == null) {
            return Decision.reject(400, "NO_IDENTITY");
        }
        return Decision.accept(requestedCanonical);
    }

    /** Valor controlado por el cliente: sin saltos de linea ni caracteres raros y acotado, para el log. */
    private static String safe(String value) {
        if (value == null) {
            return "-";
        }
        String clean = value.replaceAll("[^A-Za-z0-9_.@-]", "?");
        return clean.length() > 64 ? clean.substring(0, 64) : clean;
    }

    private static String queryParam(URI uri, String name) {
        if (uri == null || uri.getRawQuery() == null) {
            return null;
        }
        for (String param : uri.getRawQuery().split("&")) {
            int eq = param.indexOf('=');
            if (eq <= 0 || eq == param.length() - 1) {
                continue;
            }
            if (name.equalsIgnoreCase(param.substring(0, eq))) {
                String value = URLDecoder.decode(param.substring(eq + 1), StandardCharsets.UTF_8).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }
}
