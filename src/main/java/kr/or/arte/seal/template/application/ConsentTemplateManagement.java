package kr.or.arte.seal.template.application;

import java.util.List;
import java.util.UUID;

public interface ConsentTemplateManagement {
    UUID create(CreateConsentTemplateCommand command);
    void updateDraft(UpdateConsentTemplateDraftCommand command);
    void requestReview(UUID templateId, UUID versionId, UUID actorId);
    void approve(UUID templateId, UUID versionId, UUID actorId);
    void reject(UUID templateId, UUID versionId, UUID actorId, String comment);
    void publish(UUID templateId, UUID versionId, UUID actorId);
    UUID createNextDraft(UUID templateId, UUID sourceVersionId, UUID actorId);
    void updateStatus(UUID templateId, String status, long versionNo, UUID actorId);
    void deleteTemplates(List<UUID> templateIds, String reason, UUID actorId);
    void restoreTemplates(List<UUID> templateIds, UUID actorId);
}
