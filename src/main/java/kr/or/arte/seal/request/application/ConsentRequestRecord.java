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
    public ConsentRequestRecord withDraftReferences(UUID changedRecipientId,
            UUID changedProcessingActivityId, UUID changedProcessingActivityVersionId,
            UUID changedTemplateId, UUID changedTemplateVersionId,
            UUID changedRetentionVersionId, Instant changedExpiresAt, long changedVersionNo) {
        return new ConsentRequestRecord(requestId, requestCode, organizationUnitId, changedRecipientId,
                changedProcessingActivityId, changedProcessingActivityVersionId, changedTemplateId,
                changedTemplateVersionId, changedRetentionVersionId, status, changedExpiresAt,
                idempotencyKey, currentSecureLinkId, snapshotId, createdBy, createdAt, changedVersionNo);
    }
}
