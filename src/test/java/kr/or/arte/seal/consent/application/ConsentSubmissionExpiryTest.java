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
    void rejectsWhenDeadlinePassesWhileWaitingForTheRequestLock() {
        Instant before=Instant.parse("2026-09-29T00:00:00Z"), deadline=before.plusSeconds(1);
        var repository=mock(PublicConsentRepository.class);
        var clock=mock(Clock.class);
        given(clock.instant()).willReturn(before,deadline);
        var session=new PublicConsentSessionView(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),
                "ACTIVE",deadline,"{}","a".repeat(64),ConsentRequestStatus.ACTIVE);
        given(repository.findSessionForUpdateCandidates(anyList(),eq(before))).willReturn(Optional.of(session));
        byte[] key=new byte[32];Arrays.fill(key,(byte)8);
        var service=new ConsentSubmissionService(repository,new SecureLinkHasher(key),command->{},
                (response,request,at)->{throw new AssertionError("Late response queued");},clock);
        assertThatThrownBy(()->service.submit("flow",Map.of(),UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("만료");
        org.mockito.Mockito.verify(repository,org.mockito.Mockito.never()).insertResponse(org.mockito.ArgumentMatchers.any(),anyList());
    }
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
