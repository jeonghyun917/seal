package kr.or.arte.seal.evidence.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvidenceWorkItem(UUID packageId, UUID responseId, UUID requestId, UUID organizationId,
        String title, String snapshotJson, Instant submittedAt, List<String> decisions,
        List<String> events, int attemptCount) { }
