package kr.or.arte.seal.policy.persistence;

import java.util.UUID;

public record RetentionPolicyInsert(UUID retentionPolicyId, String policyCode, String policyName,
        UUID ownerOrganizationUnitId, UUID actorId) {
}
