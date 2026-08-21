package kr.or.arte.seal.policy.application.activity;

import java.util.List;
import java.util.UUID;

public interface ProcessingActivityQuery {
    List<ProcessingActivitySummary> findVisibleActivities(UUID adminUserId);
    ProcessingActivityDetail findVisibleActivity(UUID adminUserId, UUID processingActivityId);
    List<ProcessingActivityVersionView> findVisibleVersions(UUID adminUserId, UUID processingActivityId);
    List<ProcessingDataItemView> findVisibleDataItems(UUID adminUserId, UUID processingActivityId);
    List<ApprovedRetentionPolicyOption> findApprovedRetentionPolicies(UUID adminUserId);
}
