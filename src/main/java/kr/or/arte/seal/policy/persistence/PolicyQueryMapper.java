package kr.or.arte.seal.policy.persistence;

import kr.or.arte.seal.policy.application.activity.ProcessingActivitySummary;
import kr.or.arte.seal.policy.application.retention.RetentionPolicySummary;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyDetail;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyVersionView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.UUID;

@Mapper
public interface PolicyQueryMapper {
    List<RetentionPolicySummary> findVisibleRetentionPolicies(@Param("adminUserId") UUID adminUserId);
    List<RetentionPolicyDetail> findVisibleRetentionPolicy(@Param("adminUserId") UUID adminUserId,
            @Param("retentionPolicyId") UUID retentionPolicyId);
    List<RetentionPolicyVersionView> findVisibleRetentionPolicyVersions(@Param("adminUserId") UUID adminUserId,
            @Param("retentionPolicyId") UUID retentionPolicyId);
    List<ProcessingActivitySummary> findVisibleProcessingActivities(@Param("adminUserId") UUID adminUserId);
}
