package kr.or.arte.seal.policy.persistence.activity;

import java.util.UUID;

public record ProcessingActivityInsert(UUID processingActivityId, String activityCode,
        String activityName, UUID ownerOrganizationUnitId, UUID actorId) {
}
