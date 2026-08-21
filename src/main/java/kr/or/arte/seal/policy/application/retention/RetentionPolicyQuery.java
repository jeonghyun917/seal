package kr.or.arte.seal.policy.application.retention;

import java.util.List;
import java.util.UUID;

public interface RetentionPolicyQuery {
    List<RetentionPolicySummary> findVisiblePolicies(UUID adminUserId);
}
