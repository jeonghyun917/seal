package kr.or.arte.seal.template.application;

import java.util.UUID;
import java.util.List;

public interface PublishedConsentTemplateQuery {
    default java.util.Map<UUID,PublishedConsentTemplateOption> findVersionOptions(UUID actorId,List<UUID> ids) {
        return ids.stream().filter(java.util.Objects::nonNull).distinct().map(id->findVersionOption(actorId,id))
                .flatMap(java.util.Optional::stream).collect(java.util.stream.Collectors.toMap(PublishedConsentTemplateOption::templateVersionId,v->v));
    }
    PublishedConsentTemplateReference requirePublished(UUID actorId, UUID templateVersionId);
    default List<PublishedConsentTemplateOption> findAllPublished(UUID actorId) { return List.of(); }
    default java.util.Optional<PublishedConsentTemplateOption> findVersionOption(UUID actorId, UUID templateVersionId) { return java.util.Optional.empty(); }
}
