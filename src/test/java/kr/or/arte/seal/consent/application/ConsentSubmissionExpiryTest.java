package kr.or.arte.seal.consent.application;

import kr.or.arte.seal.link.application.SecureLinkHasher;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConsentSubmissionExpiryTest {
    @Test
    void submittingAnExpiredFlowDestroysItsLookupSecretImmediately() {
        Instant now = Instant.parse("2026-08-25T03:00:00Z");
        PublicConsentRepository repository = mock(PublicConsentRepository.class);
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 8);
        SecureLinkHasher hasher = new SecureLinkHasher(key);
        PublicConsentSessionView session = new PublicConsentSessionView(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "ACTIVE", now.minusSeconds(1), "{}", "a".repeat(64),
                ConsentRequestStatus.ACTIVE);
        given(repository.findSessionForUpdateCandidates(anyList(), eq(now))).willReturn(Optional.of(session));
        given(repository.findResponse(eq(session.requestId()), org.mockito.ArgumentMatchers.any()))
                .willReturn(Optional.empty());
        var service = new ConsentSubmissionService(repository, hasher, command -> { },
                (responseId, requestId, at) -> { }, Clock.fixed(now, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.submit("flow", Map.of(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("만료");

        verify(repository).expireSession(session.sessionId(), now);
    }
}
