package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.RevokedTokenRepositoryPort;
import com.findu.security.util.TokenHashUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sesiones de token en memoria (desarrollo). Mapa por hash de access + índice por hash de refresh.
 */
@Component
@ConditionalOnProperty(name = "findu.aws.dynamodb.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRevokedTokenRepositoryAdapter implements RevokedTokenRepositoryPort {

    private static final class SessionEntry {
        final String accessJwt;
        final String refreshJwt;
        volatile boolean available;
        final long ttlEpochSeconds;

        SessionEntry(String accessJwt, String refreshJwt, boolean available, long ttlEpochSeconds) {
            this.accessJwt = accessJwt;
            this.refreshJwt = refreshJwt;
            this.available = available;
            this.ttlEpochSeconds = ttlEpochSeconds;
        }
    }

    private final ConcurrentHashMap<String, SessionEntry> byAccessHash = new ConcurrentHashMap<>();
    /** refresh_hash -> access_hash */
    private final ConcurrentHashMap<String, String> refreshToAccess = new ConcurrentHashMap<>();
    /** Lista negra legada: hash -> exp */
    private final ConcurrentHashMap<String, Long> legacyRevokedUntilEpoch = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> saveTokenPair(String accessJwt, String refreshJwt, long ttlEpochSeconds) {
        return Mono.fromRunnable(() -> {
                    if (accessJwt == null || accessJwt.isBlank() || refreshJwt == null || refreshJwt.isBlank()) {
                        return;
                    }
                    String ah = TokenHashUtils.sha256Hex(accessJwt);
                    String rh = TokenHashUtils.sha256Hex(refreshJwt);
                    purgeExpiredLegacy();
                    SessionEntry e = new SessionEntry(accessJwt, refreshJwt, true, ttlEpochSeconds);
                    byAccessHash.put(ah, e);
                    refreshToAccess.put(rh, ah);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Mono<Void> markSessionUnavailable(String accessJwt, String refreshJwtOptional, long ttlEpochSeconds) {
        return Mono.fromRunnable(() -> {
                    if (accessJwt == null || accessJwt.isBlank()) {
                        return;
                    }
                    String ah = TokenHashUtils.sha256Hex(accessJwt);
                    SessionEntry e = byAccessHash.get(ah);
                    if (e != null) {
                        e.available = false;
                        return;
                    }
                    SessionEntry stub = new SessionEntry(
                            accessJwt,
                            refreshJwtOptional != null ? refreshJwtOptional : "",
                            false,
                            ttlEpochSeconds
                    );
                    byAccessHash.put(ah, stub);
                    if (refreshJwtOptional != null && !refreshJwtOptional.isBlank()) {
                        refreshToAccess.put(TokenHashUtils.sha256Hex(refreshJwtOptional), ah);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Mono<Boolean> isAccessBlocked(String accessJwt) {
        return Mono.fromCallable(() -> {
                    if (accessJwt == null || accessJwt.isBlank()) {
                        return false;
                    }
                    purgeExpiredLegacy();
                    String h = TokenHashUtils.sha256Hex(accessJwt);
                    SessionEntry e = byAccessHash.get(h);
                    if (e != null) {
                        return !e.available;
                    }
                    long now = Instant.now().getEpochSecond();
                    Long exp = legacyRevokedUntilEpoch.get(h);
                    return exp != null && now < exp;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> isRefreshBlocked(String refreshJwt) {
        return Mono.fromCallable(() -> {
                    if (refreshJwt == null || refreshJwt.isBlank()) {
                        return false;
                    }
                    purgeExpiredLegacy();
                    String rh = TokenHashUtils.sha256Hex(refreshJwt);
                    String ah = refreshToAccess.get(rh);
                    if (ah != null) {
                        SessionEntry e = byAccessHash.get(ah);
                        return e != null && !e.available;
                    }
                    long now = Instant.now().getEpochSecond();
                    Long exp = legacyRevokedUntilEpoch.get(rh);
                    return exp != null && now < exp;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> rotateSession(String oldRefreshJwt, String newAccessJwt, String newRefreshJwt, long ttlEpochSeconds) {
        return Mono.fromRunnable(() -> {
                    if (newAccessJwt == null || newAccessJwt.isBlank() || newRefreshJwt == null || newRefreshJwt.isBlank()) {
                        return;
                    }
                    purgeExpiredLegacy();
                    if (oldRefreshJwt != null && !oldRefreshJwt.isBlank()) {
                        String oldRh = TokenHashUtils.sha256Hex(oldRefreshJwt);
                        String ah = refreshToAccess.remove(oldRh);
                        if (ah != null) {
                            byAccessHash.remove(ah);
                        }
                    }
                    String nah = TokenHashUtils.sha256Hex(newAccessJwt);
                    String nrh = TokenHashUtils.sha256Hex(newRefreshJwt);
                    SessionEntry e = new SessionEntry(newAccessJwt, newRefreshJwt, true, ttlEpochSeconds);
                    byAccessHash.put(nah, e);
                    refreshToAccess.put(nrh, nah);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void purgeExpiredLegacy() {
        long now = Instant.now().getEpochSecond();
        legacyRevokedUntilEpoch.entrySet().removeIf(en -> en.getValue() != null && now >= en.getValue());
    }
}
