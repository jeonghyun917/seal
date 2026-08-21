package kr.or.arte.seal.policy.persistence;

import kr.or.arte.seal.policy.domain.retention.DestructionMethod;
import kr.or.arte.seal.policy.domain.retention.RetentionPeriodUnit;
import kr.or.arte.seal.policy.domain.retention.RetentionStartEvent;

import java.util.UUID;

public record RetentionPolicyVersionInsert(UUID retentionPolicyVersionId, UUID retentionPolicyId,
        int versionNumber, RetentionStartEvent startEvent, int periodValue,
        RetentionPeriodUnit periodUnit, int gracePeriodDays,
        DestructionMethod destructionMethod, UUID actorId) {
}
