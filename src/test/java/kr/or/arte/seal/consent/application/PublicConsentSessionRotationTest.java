package kr.or.arte.seal.consent.application;

import kr.or.arte.seal.link.application.SecureLinkHasher;
import kr.or.arte.seal.link.application.SecureTokenGenerator;
import kr.or.arte.seal.link.domain.SecureLinkStatus;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PublicConsentSessionRotationTest {
    @Test
    void startingAFlowExpiresExistingActiveSessionsBeforeCreatingTheReplacement() {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 3);
        SecureLinkHasher hasher = new SecureLinkHasher(key);
        String token = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        UUID requestId = UUID.randomUUID();
        TrackingRepository repository = new TrackingRepository(hasher.hmac(token),
                new PublicLinkRecord(UUID.randomUUID(), requestId, SecureLinkStatus.ACCESSED,
                        Instant.parse("2026-08-25T00:00:00Z"), ConsentRequestStatus.ACTIVE));
        var service = new PublicConsentEntryService(repository, hasher,
                new SecureTokenGenerator(new SecureRandom()),
                Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC));

        PublicConsentFlow flow = service.start(token);

        assertThat(flow.requestId()).isEqualTo(requestId);
        assertThat(repository.operations).isEqualTo("expire,insert");
    }

    private static final class TrackingRepository implements PublicConsentRepository {
        private final String expectedHash;
        private final PublicLinkRecord row;
        private String operations = "";

        private TrackingRepository(String expectedHash, PublicLinkRecord row) {
            this.expectedHash = expectedHash;
            this.row = row;
        }

        @Override public Optional<PublicLinkRecord> findLink(String hash) { return Optional.empty(); }
        @Override public Optional<PublicLinkRecord> findLinkForUpdate(String hash) {
            return expectedHash.equals(hash) ? Optional.of(row) : Optional.empty();
        }
        @Override public void markAccessed(UUID linkId, Instant now) { }
        @Override public void expireSessions(UUID requestId, Instant now) { append("expire"); }
        @Override public void insertSession(PublicConsentSessionRecord session) { append("insert"); }

        private void append(String operation) {
            operations = operations.isEmpty() ? operation : operations + "," + operation;
        }
    }
}
