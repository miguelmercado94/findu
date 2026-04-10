package com.findu.security.application.service;

import com.findu.security.application.port.output.cache.TokenRevocationCachePort;
import com.findu.security.application.port.output.persistence.RevokedTokenRepositoryPort;
import com.findu.security.domain.model.CachedTokenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenRevocationServiceTest {

    @Mock
    private RevokedTokenRepositoryPort revokedTokenRepositoryPort;
    @Mock
    private TokenRevocationCachePort tokenRevocationCachePort;
    @Mock
    private JwtService jwtService;

    private JwtTokenRevocationService service;

    @BeforeEach
    void setUp() {
        service = new JwtTokenRevocationService(revokedTokenRepositoryPort, tokenRevocationCachePort, jwtService);
    }

    @Test
    void isRevoked_nullOrBlank_false() {
        StepVerifier.create(service.isRevoked(null)).expectNext(false).verifyComplete();
        StepVerifier.create(service.isRevoked("  ")).expectNext(false).verifyComplete();
    }

    @Test
    void isRevoked_refresh_cacheHit_true() {
        when(jwtService.isRefreshToken("r")).thenReturn(true);
        when(tokenRevocationCachePort.isRefreshRevokedInCache("r")).thenReturn(Mono.just(true));

        StepVerifier.create(service.isRevoked("r")).expectNext(true).verifyComplete();
        verify(revokedTokenRepositoryPort, never()).isAccessBlocked(any());
    }

    @Test
    void isRevoked_refresh_cacheMiss_checksDynamo() {
        when(jwtService.isRefreshToken("r")).thenReturn(true);
        when(tokenRevocationCachePort.isRefreshRevokedInCache("r")).thenReturn(Mono.just(false));
        when(revokedTokenRepositoryPort.isRefreshBlocked("r")).thenReturn(Mono.just(true));

        StepVerifier.create(service.isRevoked("r")).expectNext(true).verifyComplete();
    }

    @Test
    void isRevoked_access_cacheHit_true() {
        when(jwtService.isRefreshToken("a")).thenReturn(false);
        when(tokenRevocationCachePort.isAccessRevokedInCache("a")).thenReturn(Mono.just(true));

        StepVerifier.create(service.isRevoked("a")).expectNext(true).verifyComplete();
    }

    @Test
    void isRevoked_access_cacheMiss_checksDynamo() {
        when(jwtService.isRefreshToken("a")).thenReturn(false);
        when(tokenRevocationCachePort.isAccessRevokedInCache("a")).thenReturn(Mono.just(false));
        when(revokedTokenRepositoryPort.isAccessBlocked("a")).thenReturn(Mono.just(false));

        StepVerifier.create(service.isRevoked("a")).expectNext(false).verifyComplete();
    }

    @Test
    void registerIssuedPair_blank_noop() {
        StepVerifier.create(service.registerIssuedPair(null, "r")).verifyComplete();
        StepVerifier.create(service.registerIssuedPair("a", null)).verifyComplete();
        verify(revokedTokenRepositoryPort, never()).saveTokenPair(any(), any(), anyLong());
    }

    @Test
    void registerIssuedPair_delegates() {
        Instant expA = Instant.parse("2030-01-01T00:00:00Z");
        Instant expR = Instant.parse("2030-06-01T00:00:00Z");
        when(jwtService.getExpiration("acc")).thenReturn(expA);
        when(jwtService.getExpiration("ref")).thenReturn(expR);
        when(revokedTokenRepositoryPort.saveTokenPair("acc", "ref", expR.getEpochSecond())).thenReturn(Mono.empty());

        StepVerifier.create(service.registerIssuedPair("acc", "ref")).verifyComplete();
    }

    @Test
    void markSessionUnavailable_blank_access_noop() {
        StepVerifier.create(service.markSessionUnavailable(null, null)).verifyComplete();
        verify(revokedTokenRepositoryPort, never()).markSessionUnavailable(any(), any(), anyLong());
    }

    @Test
    void markSessionUnavailable_withRefresh_valid() {
        when(jwtService.getExpiration("acc")).thenReturn(Instant.ofEpochSecond(100));
        when(jwtService.isValid("ref")).thenReturn(true);
        when(jwtService.getExpiration("ref")).thenReturn(Instant.ofEpochSecond(200));
        when(revokedTokenRepositoryPort.markSessionUnavailable(eq("acc"), eq("ref"), eq(200L)))
                .thenReturn(Mono.empty());
        when(tokenRevocationCachePort.putRevokedSession(any(CachedTokenSession.class), eq(200L)))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.markSessionUnavailable("acc", "ref")).verifyComplete();
    }

    @Test
    void markSessionUnavailable_refreshInvalid_usesAccessTtl() {
        when(jwtService.getExpiration("acc")).thenReturn(Instant.ofEpochSecond(100));
        when(jwtService.isValid("bad")).thenReturn(false);
        when(revokedTokenRepositoryPort.markSessionUnavailable(eq("acc"), eq("bad"), eq(100L)))
                .thenReturn(Mono.empty());
        when(tokenRevocationCachePort.putRevokedSession(any(CachedTokenSession.class), eq(100L)))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.markSessionUnavailable("acc", "bad")).verifyComplete();
    }

    @Test
    void rotateSession_blank_noop() {
        StepVerifier.create(service.rotateSession("old", null, "nr")).verifyComplete();
        verify(revokedTokenRepositoryPort, never()).rotateSession(any(), any(), any(), anyLong());
    }

    @Test
    void rotateSession_delegates() {
        when(jwtService.getExpiration("na")).thenReturn(Instant.ofEpochSecond(50));
        when(jwtService.getExpiration("nr")).thenReturn(Instant.ofEpochSecond(99));
        when(revokedTokenRepositoryPort.rotateSession("old", "na", "nr", 99)).thenReturn(Mono.empty());

        StepVerifier.create(service.rotateSession("old", "na", "nr")).verifyComplete();
    }
}
