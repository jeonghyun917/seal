package kr.or.arte.seal.policy.application.activity;

import java.util.UUID;

public record ProcessingActivitySummary(UUID processingActivityId, String activityCode, String activityName,
        String organizationName, String activityStatus, Integer approvedVersionNumber,
        String legalBasisType, String processingPurpose) {
}
