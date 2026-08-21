package kr.or.arte.seal.policy.application.retention;

import java.util.UUID;

public record RetentionPolicyDetail(UUID retentionPolicyId, String policyCode, String policyName,
        UUID ownerOrganizationUnitId, String organizationName, String policyStatus,
        UUID currentApprovedVersionId, long versionNo) {
}
