package kr.or.arte.seal.template.application;

import java.util.UUID;

public interface PublishedConsentTemplateQuery {
    PublishedConsentTemplateReference requirePublished(UUID actorId, UUID templateVersionId);
}
