package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.domain.ConsentTemplateVersion;

import java.time.Instant;
import java.util.UUID;

public interface ConsentTemplateRepository {
    void insertTemplate(ConsentTemplateDetail draft, UUID actorId, Instant now);
    ConsentTemplateDetail requireVersionForUpdate(UUID actorId, UUID templateId, UUID versionId);
    void replaceDraft(ConsentTemplateVersion draft, UUID actorId, Instant now);
}
