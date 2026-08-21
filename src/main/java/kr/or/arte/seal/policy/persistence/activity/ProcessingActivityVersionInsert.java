package kr.or.arte.seal.policy.persistence.activity;

import kr.or.arte.seal.policy.domain.activity.LegalBasisType;

import java.util.UUID;

public record ProcessingActivityVersionInsert(UUID processingActivityVersionId,
        UUID processingActivityId, int versionNumber, String processingPurpose,
        LegalBasisType legalBasisType, String legalBasisDetail,
        UUID retentionPolicyVersionId, UUID actorId) {
}
