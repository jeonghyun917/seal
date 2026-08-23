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
}
