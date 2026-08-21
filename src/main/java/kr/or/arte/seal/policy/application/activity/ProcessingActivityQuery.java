package kr.or.arte.seal.policy.application.activity;

import java.util.List;
import java.util.UUID;

public interface ProcessingActivityQuery {
    List<ProcessingActivitySummary> findVisibleActivities(UUID adminUserId);
}
