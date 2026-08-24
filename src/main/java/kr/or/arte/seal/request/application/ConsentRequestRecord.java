package kr.or.arte.seal.request.application;

import kr.or.arte.seal.request.domain.ConsentRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record ConsentRequestRecord(UUID requestId, String requestCode, UUID organizationUnitId,
        UUID recipientId, UUID processingActivityId, UUID processingActivityVersionId,
        UUID templateId, UUID templateVersionId, UUID retentionPolicyVersionId,
        ConsentRequestStatus status, Instant expiresAt, UUID idempotencyKey,
        UUID currentSecureLinkId, UUID snapshotId, UUID createdBy, Instant createdAt,
        long versionNo) {
    public ConsentRequestRecord withStatus(ConsentRequestStatus changed, UUID changedSnapshotId,
                                           long changedVersionNo) {
        return new ConsentRequestRecord(requestId, requestCode, organizationUnitId, recipientId,
                processingActivityId, processingActivityVersionId, templateId, templateVersionId,
                retentionPolicyVersionId, changed, expiresAt, idempotencyKey,
                currentSecureLinkId, changedSnapshotId, createdBy, createdAt, changedVersionNo);
    }
}
