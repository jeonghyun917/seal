package kr.or.arte.seal.template.persistence;

import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityQuery;
import kr.or.arte.seal.template.application.ConsentTemplateRepository;
import kr.or.arte.seal.template.application.PublishedConsentTemplateQuery;
import kr.or.arte.seal.template.application.PublishedConsentTemplateReference;
import kr.or.arte.seal.template.application.TemplatePreviewRenderer;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.List;
import kr.or.arte.seal.template.application.PublishedConsentTemplateOption;

@Repository
public class MyBatisPublishedConsentTemplateQuery implements PublishedConsentTemplateQuery {
    private final ConsentTemplateQueryMapper mapper;
    private final ConsentTemplateRepository repository;
    private final ApprovedProcessingActivityQuery activityQuery;
    private final TemplatePreviewRenderer renderer;

    public MyBatisPublishedConsentTemplateQuery(ConsentTemplateQueryMapper mapper,
            ConsentTemplateRepository repository, ApprovedProcessingActivityQuery activityQuery,
            TemplatePreviewRenderer renderer) {
        this.mapper = mapper;
        this.repository = repository;
        this.activityQuery = activityQuery;
        this.renderer = renderer;
    }

    @Override
    @Transactional(readOnly = true)
    public PublishedConsentTemplateReference requirePublished(UUID actorId, UUID templateVersionId) {
        PublishedTemplateIdentityRow identity = mapper.findPublishedIdentity(actorId, templateVersionId)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "요청에 사용할 수 있는 동의서 양식이 아닙니다."));
        var detail = repository.requireVersion(actorId, identity.templateId(), templateVersionId);
        var activity = activityQuery.findReferenceForExistingTemplate(actorId,
                detail.version().processingActivityVersionId());
        return new PublishedConsentTemplateReference(identity.templateId(), templateVersionId,
                detail.processingActivityId(), detail.version().processingActivityVersionId(),
                activity.retentionPolicyVersionId(), activity.retentionDisplayText(),
                detail.version().contentHash(), renderer.toView(detail, activity),
                detail.version().items().stream().collect(Collectors.toUnmodifiableMap(
                        item -> item.order(), item -> item.processingDataItemIds())));
    }

    @Override
    public List<PublishedConsentTemplateOption> findAllPublished(UUID actorId) {
        return mapper.findPublishedOptions(actorId);
    }

    @Override public java.util.Map<UUID,PublishedConsentTemplateOption> findVersionOptions(UUID actorId,List<UUID> versionIds) {
        var ids=versionIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if(ids.isEmpty()) return java.util.Map.of();
        if(ids.size()>1000) throw new IllegalArgumentException("양식 일괄 조회는 1,000개 이하여야 합니다.");
        return mapper.findVersionOptions(actorId,ids).stream().collect(Collectors.toMap(PublishedConsentTemplateOption::templateVersionId,v->v));
    }

    @Override
    public java.util.Optional<PublishedConsentTemplateOption> findVersionOption(
            UUID actorId, UUID templateVersionId) {
        return mapper.findVersionOption(actorId, templateVersionId).stream().findFirst();
    }
}
