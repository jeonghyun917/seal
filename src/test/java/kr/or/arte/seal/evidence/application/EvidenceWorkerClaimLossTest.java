package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceWorkerClaimLossTest {
    @Test
    void lostFirstClaimDoesNotStopTheSecondPackage() {
        Instant now = Instant.parse("2026-08-25T03:00:00Z");
        ClaimLossRepository repository = new ClaimLossRepository(now);
        var generated = new GeneratedEvidence("org/request/package", "a".repeat(64),
                "hmac", "v1", List.of());
        var worker = new EvidenceWorker(repository, item -> generated, true,
                Clock.fixed(now, ZoneOffset.UTC));

        assertThat(worker.processBatch(10)).isEqualTo(2);
        assertThat(repository.finalizedIds).containsExactly(repository.second.packageId());
        assertThat(repository.failedIds).isEmpty();
    }

    private static final class ClaimLossRepository implements EvidenceRepository {
        private final EvidenceWorkItem first;
        private final EvidenceWorkItem second;
        private final List<UUID> finalizedIds = new ArrayList<>();
        private final List<UUID> failedIds = new ArrayList<>();

        private ClaimLossRepository(Instant now) {
            first = item(now, 7);
            second = item(now, 9);
        }

        public void enqueue(UUID responseId, UUID requestId, Instant queuedAt) { }
        public List<EvidenceWorkItem> claimDue(Instant now, int limit, Instant leaseExpiredAt) {
            return List.of(first, second);
        }
        public void finalized(UUID id, long version, GeneratedEvidence generated, Instant now) {
            if (id.equals(first.packageId())) throw new EvidenceClaimLostException();
            finalizedIds.add(id);
        }
        public void failed(UUID id, long version, String code, Instant now) {
            failedIds.add(id);
        }
        public List<EvidencePackageSummary> search(UUID actorId, EvidenceGenerationStatus status) {
            return List.of();
        }
        public Optional<EvidencePackageDetail> findScoped(UUID actorId, UUID packageId) {
            return Optional.empty();
        }
        public void requeue(UUID actorId, UUID packageId, Instant now) { }
        public void recordVerification(UUID packageId, UUID actorId,
                EvidenceVerificationResult result, Instant now) { }

        private static EvidenceWorkItem item(Instant now, long version) {
            return new EvidenceWorkItem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), "Consent evidence", "{}", now, List.of("AGREE"),
                    List.of("SUBMITTED"), 0, version);
        }
    }
}
