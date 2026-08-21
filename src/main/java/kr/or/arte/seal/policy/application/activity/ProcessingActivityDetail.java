package kr.or.arte.seal.policy.application.activity;

import java.util.UUID;

public record ProcessingActivityDetail(UUID processingActivityId, String activityCode,
        String activityName, UUID ownerOrganizationUnitId, String organizationName,
        String activityStatus, UUID currentApprovedVersionId, long versionNo) {
}
