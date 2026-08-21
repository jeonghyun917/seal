package kr.or.arte.seal.policy.persistence.activity;

import kr.or.arte.seal.policy.domain.activity.LegalBasisType;
import kr.or.arte.seal.policy.domain.activity.ProcessingWorkflowStatus;

import java.util.UUID;

public record ProcessingActivityVersionRow(UUID processingActivityVersionId,
        UUID processingActivityId, int versionNumber, ProcessingWorkflowStatus workflowStatus,
        String processingPurpose, LegalBasisType legalBasisType, String legalBasisDetail,
        UUID retentionPolicyVersionId, UUID createdBy, UUID reviewRequestedBy,
        UUID approvedBy, String rejectionComment, long versionNo) {
}
