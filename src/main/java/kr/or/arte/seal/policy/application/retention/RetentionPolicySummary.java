package kr.or.arte.seal.policy.application.retention;

import java.util.UUID;

public record RetentionPolicySummary(UUID retentionPolicyId, String policyCode, String policyName,
        String organizationName, String policyStatus, Integer approvedVersionNumber,
        String startEvent, Integer periodValue, String periodUnit) {
}
