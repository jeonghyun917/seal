package kr.or.arte.seal.policy.persistence.activity;

import java.util.UUID;

public record ApprovedProcessingActivityRow(
        UUID activityId,
        UUID versionId,
        UUID ownerOrganizationId,
        String activityCode,
        String activityName,
        String processingPurpose,
        String legalBasisType,
        UUID retentionPolicyVersionId,
        String retentionDisplayText
) {
}
