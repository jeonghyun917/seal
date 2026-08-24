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
                        "요청에 사용할 수 있는 발행 템플릿이 아닙니다."));
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
}
