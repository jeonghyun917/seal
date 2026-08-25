package kr.or.arte.seal.retention.application;

import kr.or.arte.seal.retention.domain.RetentionJobStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RetentionJobRecord(UUID id, UUID consentRequestId, UUID recipientId,
        UUID policyVersionId, UUID sourceRightsRequestId, LocalDate anchorDate,
        LocalDate dueDate, RetentionJobStatus status, String destructionMethod,
        UUID idempotencyKey, UUID requestedBy, Instant requestedAt, UUID approvedBy,
        UUID executedBy, UUID confirmedBy, int destroyedMailCount, int destroyedFileCount,
        int destroyedSessionCount, int destroyedRightsCount, boolean recipientPiiDestroyed,
        long rowVersion) {

    public boolean executionAvailable() {
        return status == RetentionJobStatus.APPROVED || status == RetentionJobStatus.EXECUTING;
    }

    public UUID executionKey() {
        if (status == RetentionJobStatus.APPROVED) {
            return id;
        }
        if (status == RetentionJobStatus.EXECUTING) {
            return idempotencyKey;
        }
        return null;
    }
}
