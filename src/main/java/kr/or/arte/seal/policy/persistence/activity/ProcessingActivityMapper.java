package kr.or.arte.seal.policy.persistence.activity;

import kr.or.arte.seal.policy.domain.activity.ProcessingActivityVersion;
import kr.or.arte.seal.policy.domain.activity.ProcessingDataItem;
import kr.or.arte.seal.policy.application.activity.ApprovedDataItemReference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ProcessingActivityMapper {
    boolean canAccessActiveOrganization(@Param("adminUserId") UUID adminUserId,
            @Param("organizationUnitId") UUID organizationUnitId);
    boolean canUseApprovedRetentionVersion(@Param("adminUserId") UUID adminUserId,
            @Param("retentionPolicyVersionId") UUID retentionPolicyVersionId);
    int insertActivity(ProcessingActivityInsert activity);
    int insertVersion(ProcessingActivityVersionInsert version);
    int insertDataItems(@Param("items") List<ProcessingDataItemInsert> items);
    List<ProcessingActivityVersionRow> findVisibleVersionForUpdate(
            @Param("adminUserId") UUID adminUserId,
            @Param("processingActivityId") UUID processingActivityId,
            @Param("processingActivityVersionId") UUID processingActivityVersionId);
    List<ProcessingDataItem> findDataItems(
            @Param("processingActivityVersionId") UUID processingActivityVersionId);
    int updateVersionTransition(@Param("version") ProcessingActivityVersion version,
            @Param("actorId") UUID actorId, @Param("occurredAt") Instant occurredAt);
    int supersedePreviousApprovedVersions(@Param("processingActivityId") UUID processingActivityId,
            @Param("approvedVersionId") UUID approvedVersionId, @Param("actorId") UUID actorId);
    int updateCurrentApprovedVersion(@Param("processingActivityId") UUID processingActivityId,
            @Param("approvedVersionId") UUID approvedVersionId, @Param("actorId") UUID actorId);
    List<ApprovedProcessingActivityRow> findApprovedForTemplate(
            @Param("adminUserId") UUID adminUserId,
            @Param("processingActivityVersionId") UUID processingActivityVersionId);
    List<ApprovedProcessingActivityRow> findAllApprovedForTemplate(
            @Param("adminUserId") UUID adminUserId);
    List<ApprovedProcessingActivityRow> findReferenceForExistingTemplate(
            @Param("adminUserId") UUID adminUserId,
            @Param("processingActivityVersionId") UUID processingActivityVersionId);
    List<ApprovedDataItemReference> findApprovedDataItems(
            @Param("processingActivityVersionId") UUID processingActivityVersionId);
}
