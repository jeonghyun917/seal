package kr.or.arte.seal.policy.application.activity;

import kr.or.arte.seal.policy.domain.activity.LegalBasisType;
import kr.or.arte.seal.policy.domain.activity.ProcessingDataItem;

import java.util.List;
import java.util.UUID;

public record ProcessingActivityCreateCommand(
        String activityCode,
        String activityName,
        UUID ownerOrganizationUnitId,
        String processingPurpose,
        LegalBasisType legalBasisType,
        String legalBasisDetail,
        UUID retentionPolicyVersionId,
        List<ProcessingDataItem> dataItems,
        UUID actorId
) {
}
