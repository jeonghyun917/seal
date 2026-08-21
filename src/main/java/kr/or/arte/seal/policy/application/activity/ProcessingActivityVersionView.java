package kr.or.arte.seal.policy.application.activity;

import kr.or.arte.seal.policy.domain.activity.LegalBasisType;
import kr.or.arte.seal.policy.domain.activity.ProcessingWorkflowStatus;

import java.util.UUID;

public record ProcessingActivityVersionView(UUID processingActivityVersionId,
        int versionNumber, ProcessingWorkflowStatus workflowStatus, String processingPurpose,
        LegalBasisType legalBasisType, String legalBasisDetail, String retentionPolicyName,
        int retentionVersionNumber, String createdByName, String reviewRequestedByName,
        String approvedByName, String rejectionComment) {
}
