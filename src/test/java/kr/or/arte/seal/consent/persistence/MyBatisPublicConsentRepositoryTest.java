package kr.or.arte.seal.consent.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MyBatisPublicConsentRepositoryTest {
    @Test
    void completingOneSessionExpiresAndDestroysOtherActiveSessions() {
        PublicConsentMapper mapper = mock(PublicConsentMapper.class);
        var repository = new MyBatisPublicConsentRepository(mapper);
        UUID requestId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-25T02:00:00Z");
        given(mapper.completeRequest(requestId, now)).willReturn(1);

        repository.complete(requestId, linkId, sessionId, now);

        verify(mapper).expireOtherSessions(requestId, sessionId, now);
    }
}
