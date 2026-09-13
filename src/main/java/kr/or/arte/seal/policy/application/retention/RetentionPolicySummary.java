package kr.or.arte.seal.policy.application.retention;

import kr.or.arte.seal.policy.domain.retention.RetentionPeriodUnit;

import java.util.UUID;

public record RetentionPolicySummary(UUID retentionPolicyId, String policyCode, String policyName,
        String organizationName, String policyStatus, Integer approvedVersionNumber,
        String startEvent, Integer periodValue, String periodUnit) {
    public String periodUnitLabel() {
        return periodUnit == null ? "-" : RetentionPeriodUnit.valueOf(periodUnit).getLabel();
    }
}
