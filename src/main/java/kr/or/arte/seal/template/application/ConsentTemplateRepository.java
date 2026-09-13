package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.domain.ConsentTemplateVersion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConsentTemplateRepository {
    void insertTemplate(ConsentTemplateDetail draft, UUID actorId, Instant now);
    ConsentTemplateDetail requireVersionForUpdate(UUID actorId, UUID templateId, UUID versionId);
    ConsentTemplateDetail requireVersion(UUID actorId, UUID templateId, UUID versionId);
    void replaceDraft(ConsentTemplateVersion draft, UUID actorId, Instant now);
    void updateTemplateName(UUID templateId, String templateName, long versionNo,
                            UUID actorId, Instant now);
    void updateTransition(ConsentTemplateVersion version, UUID actorId,
                          String reviewAction, String comment, Instant now);
    void publish(ConsentTemplateVersion version, UUID actorId, Instant now);
    boolean isLatestPublishedOrRetired(UUID templateId, UUID sourceVersionId);
    int nextVersionNumber(UUID templateId);
    void insertVersion(ConsentTemplateVersion draft, UUID actorId, Instant now);
    void updateStatus(UUID templateId, String status, long versionNo, UUID actorId, Instant now);
    int softDelete(List<UUID> templateIds, String reason, UUID actorId, Instant now);
    int restore(List<UUID> templateIds, UUID actorId, Instant now);
}
