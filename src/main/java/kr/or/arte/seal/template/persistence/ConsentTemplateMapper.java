package kr.or.arte.seal.template.persistence;

import kr.or.arte.seal.template.application.ConsentTemplateDetail;
import kr.or.arte.seal.template.domain.ConsentItemDefinition;
import kr.or.arte.seal.template.domain.ConsentTemplateVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ConsentTemplateMapper {
    int insertTemplate(@Param("detail") ConsentTemplateDetail detail,
                       @Param("actorId") UUID actorId, @Param("now") Instant now);
    int insertVersion(@Param("version") ConsentTemplateVersion version,
                      @Param("actorId") UUID actorId, @Param("now") Instant now);
    int insertItems(@Param("versionId") UUID versionId,
                    @Param("processingActivityVersionId") UUID processingActivityVersionId,
                    @Param("items") List<ConsentItemDefinition> items);
    int insertSelections(@Param("selections") List<ConsentDataSelectionInsert> selections);
    List<ConsentTemplateHeaderRow> findVisibleVersionForUpdate(
            @Param("actorId") UUID actorId, @Param("templateId") UUID templateId,
            @Param("versionId") UUID versionId);
    List<ConsentItemRow> findItems(@Param("versionId") UUID versionId);
    List<ConsentDataSelectionRow> findSelections(@Param("versionId") UUID versionId);
    int clearDraftItems(@Param("versionId") UUID versionId, @Param("actorId") UUID actorId);
    int updateDraft(@Param("version") ConsentTemplateVersion version,
                    @Param("actorId") UUID actorId, @Param("now") Instant now);
    int updateTemplateName(@Param("templateId") UUID templateId,
                           @Param("templateName") String templateName,
                           @Param("versionNo") long versionNo,
                           @Param("actorId") UUID actorId, @Param("now") Instant now);
    int updateTransition(@Param("version") ConsentTemplateVersion version,
                         @Param("actorId") UUID actorId, @Param("now") Instant now);
    int insertReview(@Param("reviewId") UUID reviewId, @Param("versionId") UUID versionId,
                     @Param("action") String action, @Param("actorId") UUID actorId,
                     @Param("comment") String comment, @Param("now") Instant now);
    List<UUID> findPublishedVersionIdsForUpdate(@Param("templateId") UUID templateId);
    int retirePublishedVersions(@Param("templateId") UUID templateId,
                                @Param("newVersionId") UUID newVersionId,
                                @Param("actorId") UUID actorId, @Param("now") Instant now);
    int publishVersion(@Param("version") ConsentTemplateVersion version,
                       @Param("actorId") UUID actorId, @Param("now") Instant now);
    int updateCurrentPublishedVersion(@Param("templateId") UUID templateId,
                                      @Param("versionId") UUID versionId,
                                      @Param("actorId") UUID actorId, @Param("now") Instant now);
    boolean isLatestPublishedOrRetired(@Param("templateId") UUID templateId,
                                       @Param("sourceVersionId") UUID sourceVersionId);
    int nextVersionNumber(@Param("templateId") UUID templateId);
    int updateStatus(@Param("templateId") UUID templateId, @Param("status") String status,
                     @Param("versionNo") long versionNo, @Param("actorId") UUID actorId,
                     @Param("now") Instant now);
}
