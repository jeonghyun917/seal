package kr.or.arte.seal.policy.persistence;

import kr.or.arte.seal.policy.domain.retention.RetentionPolicyVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface RetentionPolicyMapper {
    boolean canAccessOrganization(@Param("adminUserId") UUID adminUserId,
            @Param("organizationUnitId") UUID organizationUnitId);
    int insertPolicy(RetentionPolicyInsert policy);
    int insertVersion(RetentionPolicyVersionInsert version);
    List<RetentionPolicyVersion> findVisibleVersionForUpdate(
            @Param("adminUserId") UUID adminUserId,
            @Param("retentionPolicyId") UUID retentionPolicyId,
            @Param("retentionPolicyVersionId") UUID retentionPolicyVersionId);
    int updateVersionTransition(@Param("version") RetentionPolicyVersion version,
            @Param("actorId") UUID actorId, @Param("occurredAt") Instant occurredAt);
    int supersedePreviousApprovedVersions(@Param("retentionPolicyId") UUID retentionPolicyId,
            @Param("approvedVersionId") UUID approvedVersionId, @Param("actorId") UUID actorId);
    int updateCurrentApprovedVersion(@Param("retentionPolicyId") UUID retentionPolicyId,
            @Param("approvedVersionId") UUID approvedVersionId, @Param("actorId") UUID actorId);
}
