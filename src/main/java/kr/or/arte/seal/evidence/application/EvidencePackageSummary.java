package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import java.time.Instant;
import java.util.UUID;

public record EvidencePackageSummary(UUID packageId, UUID requestId, String requestCode,
        String templateTitle, UUID recipientId, int agreedCount, int decisionCount,
        EvidenceGenerationStatus status, Instant queuedAt, Instant finalizedAt,
        int attemptCount, String lastErrorCode) { }
