package kr.or.arte.seal.evidence.persistence;

import java.time.Instant;
import java.util.UUID;

public record EvidenceWorkRow(UUID packageId, UUID responseId, UUID requestId, UUID organizationId,
        String title, String snapshotJson, Instant submittedAt, int attemptCount) { }
