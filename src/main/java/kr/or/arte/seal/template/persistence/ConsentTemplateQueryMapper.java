package kr.or.arte.seal.template.persistence;

import kr.or.arte.seal.template.application.ConsentTemplateSummary;
import kr.or.arte.seal.template.application.ConsentTemplateVersionSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ConsentTemplateQueryMapper {
    List<ConsentTemplateSummary> search(@Param("actorId") UUID actorId,
                                        @Param("keyword") String keyword,
                                        @Param("status") String status);
    List<ConsentTemplatePageRow> findPage(@Param("actorId") UUID actorId,
                                          @Param("templateId") UUID templateId);
    List<ConsentTemplateVersionSummary> findVersions(@Param("actorId") UUID actorId,
                                                     @Param("templateId") UUID templateId);
    List<PublishedTemplateIdentityRow> findPublishedIdentity(@Param("actorId") UUID actorId,
                                                              @Param("templateVersionId") UUID templateVersionId);
}
