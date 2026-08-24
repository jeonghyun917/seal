package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceWorkerTest {
    private final Instant now = Instant.parse("2026-08-24T01:00:00Z");

    @Test void disabledWorkerDoesNotClaim() {
        FakeRepository repository = new FakeRepository();
        var worker = new EvidenceWorker(repository, item -> { throw new AssertionError(); }, false,
                Clock.fixed(now, ZoneOffset.UTC));
        assertThat(worker.processBatch(10)).isZero();
        assertThat(repository.claimed).isFalse();
    }

    @Test void finalizesGeneratedPackage() {
        FakeRepository repository = new FakeRepository();
        var generated = new GeneratedEvidence("org/request/package", "a".repeat(64), "hmac", List.of());
        var worker = new EvidenceWorker(repository, item -> generated, true, Clock.fixed(now, ZoneOffset.UTC));
        assertThat(worker.processBatch(10)).isOne();
        assertThat(repository.finalized).isEqualTo(generated);
        assertThat(repository.failedCode).isNull();
        assertThat(repository.lease).isEqualTo(now.minus(Duration.ofMinutes(10)));
    }

    @Test void recordsNonPersonalFailureCode() {
        FakeRepository repository = new FakeRepository();
        var worker = new EvidenceWorker(repository, item -> { throw new IllegalStateException("private detail"); },
                true, Clock.fixed(now, ZoneOffset.UTC));
        worker.processBatch(10);
        assertThat(repository.failedCode).isEqualTo("EVIDENCE_GENERATION_FAILED");
    }

    private final class FakeRepository implements EvidenceRepository {
        boolean claimed; Instant lease; GeneratedEvidence finalized; String failedCode;
        final UUID packageId = UUID.randomUUID();
        public void enqueue(UUID responseId, UUID requestId, Instant queuedAt) { }
        public List<EvidenceWorkItem> claimDue(Instant at, int limit, Instant leaseExpiredAt) {
            claimed = true; lease = leaseExpiredAt;
            return List.of(new EvidenceWorkItem(packageId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "교육 참여 동의", "{}", now, List.of("필수: AGREE"), List.of("SUBMITTED"), 0));
        }
        public void finalized(UUID id, GeneratedEvidence value, Instant at) { finalized = value; }
        public void failed(UUID id, String code, Instant at) { failedCode = code; }
        public List<EvidencePackageSummary> search(UUID actorId, EvidenceGenerationStatus status) { return List.of(); }
        public Optional<EvidencePackageDetail> findScoped(UUID actorId, UUID packageId) { return Optional.empty(); }
        public void requeue(UUID actorId, UUID packageId, Instant at) { }
        public void recordVerification(UUID packageId, UUID actorId, EvidenceVerificationResult result, Instant at) { }
    }
}
