package kr.or.arte.seal.evidence.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvidenceWorkItem(UUID packageId, UUID responseId, UUID requestId, UUID organizationId,
        String requestCode, String templateCode, String templateName, int templateVersionNumber,
        String recipientName, String recipientEmail, String recipientAffiliation,
        String recipientJobTitle, Instant requestCreatedAt, Instant firstSentAt, Instant expiresAt,
        String snapshotJson, String snapshotHash, String responseStatus, Instant submittedAt,
        List<String> decisions, List<String> events, int attemptCount, long rowVersion) {

    public EvidenceWorkItem {
        decisions = List.copyOf(decisions);
        events = List.copyOf(events);
    }

    public EvidenceWorkItem(UUID packageId, UUID responseId, UUID requestId, UUID organizationId,
            String requestCode, String snapshotJson, Instant submittedAt, List<String> decisions,
            List<String> events, int attemptCount, long rowVersion) {
        this(packageId, responseId, requestId, organizationId, requestCode, "", "", 0,
                "", "", "", "", submittedAt, null, submittedAt, snapshotJson, "", "",
                submittedAt, decisions, events, attemptCount, rowVersion);
    }
}
