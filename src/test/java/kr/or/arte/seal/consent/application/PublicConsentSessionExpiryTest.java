package kr.or.arte.seal.consent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.arte.seal.link.application.SecureLinkHasher;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PublicConsentSessionExpiryTest {
    private static final Instant NOW = Instant.parse("2026-08-25T03:00:00Z");

    @Test
    void loadingAnExpiredFlowDestroysItsLookupSecretImmediately() {
        PublicConsentRepository repository = mock(PublicConsentRepository.class);
        SecureLinkHasher hasher = hasher();
        PublicConsentSessionView session = expiredSession();
        given(repository.findSessionForUpdateCandidates(anyList(), eq(NOW))).willReturn(Optional.of(session));
        var service = new PublicConsentQueryService(repository, hasher, new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.load("flow"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료");

        verify(repository).expireSession(session.sessionId(), NOW);
    }

    private static SecureLinkHasher hasher() {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 9);
        return new SecureLinkHasher(key);
    }

    private static PublicConsentSessionView expiredSession() {
        return new PublicConsentSessionView(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "ACTIVE", NOW.minusSeconds(1), "{}", "a".repeat(64), ConsentRequestStatus.ACTIVE);
    }
}
