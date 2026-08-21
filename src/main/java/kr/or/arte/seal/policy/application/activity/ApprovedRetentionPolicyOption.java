package kr.or.arte.seal.policy.application.activity;

import java.util.UUID;

public record ApprovedRetentionPolicyOption(UUID retentionPolicyVersionId,
        String policyCode, String policyName, int versionNumber, String organizationName) {
}
