package kr.or.arte.seal.consent.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PublicConsentSessionExpiryServiceTest {
    @Test
    void expiresDueSessionsInBoundedBatches() {
        Instant now = Instant.parse("2026-08-25T03:00:00Z");
        PublicConsentRepository repository = mock(PublicConsentRepository.class);
        given(repository.expireDueSessions(now, 100)).willReturn(7);
        var service = new PublicConsentSessionExpiryService(repository,
                Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.expireDue(100)).isEqualTo(7);
        verify(repository).expireDueSessions(now, 100);
    }

    @Test
    void rejectsUnboundedBatchSizes() {
        var service = new PublicConsentSessionExpiryService(mock(PublicConsentRepository.class),
                Clock.systemUTC());

        assertThatThrownBy(() -> service.expireDue(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.expireDue(1001)).isInstanceOf(IllegalArgumentException.class);
    }
}
