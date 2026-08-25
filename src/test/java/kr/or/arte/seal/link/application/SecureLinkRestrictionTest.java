package kr.or.arte.seal.link.application;

import kr.or.arte.seal.request.application.ConsentRequestRecord;
import kr.or.arte.seal.request.application.ConsentRequestRepository;
import kr.or.arte.seal.request.application.ConsentRestrictionQuery;
import kr.or.arte.seal.request.application.RequestBusinessConflictException;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SecureLinkRestrictionTest {
    private static final Instant NOW = Instant.parse("2026-08-24T15:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID REQUEST = UUID.randomUUID();
    private static final UUID RECIPIENT = UUID.randomUUID();

    @Test
    void activeRestrictionBlocksInitialIssueAndReissueBeforeLinkMutation() {
        ConsentRequestRepository requests = mock(ConsentRequestRepository.class);
        SecureLinkRepository links = mock(SecureLinkRepository.class);
        ConsentRestrictionQuery restrictions = mock(ConsentRestrictionQuery.class);
        given(requests.findScopedForUpdate(ACTOR, REQUEST)).willReturn(
                Optional.of(request(ConsentRequestStatus.READY)),
                Optional.of(request(ConsentRequestStatus.ACTIVE)));
        given(restrictions.hasActiveRestriction(eq(RECIPIENT), eq(LocalDate.of(2026, 8, 25))))
                .willReturn(true);
        var service = new SecureLinkManagementService(requests, links,
                mock(SecureTokenGenerator.class), mock(SecureLinkHasher.class), restrictions, CLOCK);

        assertThatThrownBy(() -> service.activate(ACTOR, REQUEST, 0))
                .isInstanceOf(RequestBusinessConflictException.class)
                .hasMessageContaining("처리제한");
        assertThatThrownBy(() -> service.reissue(ACTOR, REQUEST, 0))
                .isInstanceOf(RequestBusinessConflictException.class)
                .hasMessageContaining("처리제한");
        verifyNoInteractions(links);
    }

    private static ConsentRequestRecord request(ConsentRequestStatus status) {
        return new ConsentRequestRecord(REQUEST, "CR-20260825-ABCDEFGH", UUID.randomUUID(),
                RECIPIENT, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), status, NOW.plusSeconds(3600),
                UUID.randomUUID(), null, UUID.randomUUID(), ACTOR, NOW, 0);
    }
}
