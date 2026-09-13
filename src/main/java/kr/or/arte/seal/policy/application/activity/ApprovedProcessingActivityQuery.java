package kr.or.arte.seal.policy.application.activity;

import java.util.List;
import java.util.UUID;

public interface ApprovedProcessingActivityQuery {
    ApprovedProcessingActivityReference findApprovedForTemplate(UUID actorId, UUID versionId);
    ApprovedProcessingActivityReference findReferenceForExistingTemplate(UUID actorId, UUID versionId);
    List<ApprovedProcessingActivityReference> findAllApprovedForTemplate(UUID actorId);
    List<ApprovedProcessingActivityReference> searchApprovedForTemplate(
            UUID actorId, String keyword, int limit, int offset);
}
