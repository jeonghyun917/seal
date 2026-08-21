package kr.or.arte.seal.policy.persistence;

import kr.or.arte.seal.policy.application.activity.ProcessingActivitySummary;
import kr.or.arte.seal.policy.application.retention.RetentionPolicySummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.UUID;

@Mapper
public interface PolicyQueryMapper {
    List<RetentionPolicySummary> findVisibleRetentionPolicies(@Param("adminUserId") UUID adminUserId);
    List<ProcessingActivitySummary> findVisibleProcessingActivities(@Param("adminUserId") UUID adminUserId);
}
