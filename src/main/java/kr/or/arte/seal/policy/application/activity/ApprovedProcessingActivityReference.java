package kr.or.arte.seal.policy.application.activity;

import java.util.List;
import java.util.UUID;

public record ApprovedProcessingActivityReference(
        UUID activityId,
        UUID versionId,
        UUID ownerOrganizationId,
        String activityName,
        String processingPurpose,
        String legalBasisType,
        UUID retentionPolicyVersionId,
        String retentionDisplayText,
        List<ApprovedDataItemReference> dataItems
) {
    public ApprovedProcessingActivityReference {
        dataItems = List.copyOf(dataItems);
    }
}
