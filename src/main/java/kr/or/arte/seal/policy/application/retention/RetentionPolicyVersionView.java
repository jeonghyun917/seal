package kr.or.arte.seal.policy.application.retention;

import java.util.UUID;

public record RetentionPolicyVersionView(UUID retentionPolicyVersionId, int versionNumber,
        String workflowStatus, String startEvent, int periodValue, String periodUnit,
        int gracePeriodDays, String destructionMethod, String createdByName,
        String reviewRequestedByName, String approvedByName, String rejectionComment) {
}
