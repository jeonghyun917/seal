package kr.or.arte.seal.request.application;

import kr.or.arte.seal.link.application.SecureLinkRepository;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConsentRequestExpiryServiceTest {
    @Test
    void expiringARequestDestroysAllActivePublicSessions() {
        Instant now = Instant.parse("2026-08-25T02:00:00Z");
        UUID requestId = UUID.randomUUID();
        ConsentRequestRepository requests = mock(ConsentRequestRepository.class);
        SecureLinkRepository links = mock(SecureLinkRepository.class);
        PublicConsentSessionLifecycle sessions = mock(PublicConsentSessionLifecycle.class);
        ConsentRequestRecord request = new ConsentRequestRecord(requestId, "CR-20260825-ABCDEFGH",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ConsentRequestStatus.ACTIVE,
                now.minusSeconds(1), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), now.minusSeconds(3600), 3);
        given(requests.findDueForUpdate(now, 100)).willReturn(List.of(request));
        given(requests.markExpired(requestId, now, 3)).willReturn(true);
        var service = new ConsentRequestExpiryService(requests, links, sessions,
                Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.expireDue(100)).isOne();

        verify(sessions).expireSessions(requestId, now);
    }
}
