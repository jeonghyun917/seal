package kr.or.arte.seal.template.persistence;

import kr.or.arte.seal.template.application.ConsentTemplateSummary;
import kr.or.arte.seal.template.application.DeletedConsentTemplateSummary;
import kr.or.arte.seal.template.application.ConsentTemplateVersionSummary;
import kr.or.arte.seal.template.application.PublishedConsentTemplateOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ConsentTemplateQueryMapper {
    List<PublishedConsentTemplateOption> findVersionOptions(@Param("actorId") UUID actorId,@Param("ids") List<UUID> ids);
    List<ConsentTemplateSummary> search(@Param("actorId") UUID actorId,
                                        @Param("keyword") String keyword,
                                        @Param("status") String status);
    List<DeletedConsentTemplateSummary> searchDeleted(@Param("actorId") UUID actorId,
                                                      @Param("keyword") String keyword);    List<ConsentTemplatePageRow> findPage(@Param("actorId") UUID actorId,
                                          @Param("templateId") UUID templateId);
    List<ConsentTemplateVersionSummary> findVersions(@Param("actorId") UUID actorId,
                                                     @Param("templateId") UUID templateId);
    List<PublishedTemplateIdentityRow> findPublishedIdentity(@Param("actorId") UUID actorId,
                                                              @Param("templateVersionId") UUID templateVersionId);
    List<PublishedConsentTemplateOption> findPublishedOptions(@Param("actorId") UUID actorId);
    List<PublishedConsentTemplateOption> findVersionOption(@Param("actorId") UUID actorId,
                                                           @Param("templateVersionId") UUID templateVersionId);
}
