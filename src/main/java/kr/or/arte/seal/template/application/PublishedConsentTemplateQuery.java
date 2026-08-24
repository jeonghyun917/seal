package kr.or.arte.seal.template.application;

import java.util.UUID;
import java.util.List;

public interface PublishedConsentTemplateQuery {
    PublishedConsentTemplateReference requirePublished(UUID actorId, UUID templateVersionId);
    default List<PublishedConsentTemplateOption> findAllPublished(UUID actorId) { return List.of(); }
}
