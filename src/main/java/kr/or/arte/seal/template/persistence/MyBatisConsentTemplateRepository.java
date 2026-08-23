package kr.or.arte.seal.template.persistence;

import kr.or.arte.seal.template.application.ConsentTemplateDetail;
import kr.or.arte.seal.template.application.ConsentTemplateRepository;
import kr.or.arte.seal.template.domain.ConsentItemDefinition;
import kr.or.arte.seal.template.domain.ConsentTemplateVersion;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class MyBatisConsentTemplateRepository implements ConsentTemplateRepository {
    private final ConsentTemplateMapper mapper;

    public MyBatisConsentTemplateRepository(ConsentTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void insertTemplate(ConsentTemplateDetail detail, UUID actorId, Instant now) {
        requireOne(mapper.insertTemplate(detail, actorId, now), "템플릿을 생성할 수 없습니다.");
        requireOne(mapper.insertVersion(detail.version(), actorId, now), "템플릿 초안을 생성할 수 없습니다.");
        insertItems(detail.version());
    }

    @Override
    @Transactional
    public ConsentTemplateDetail requireVersionForUpdate(UUID actorId, UUID templateId, UUID versionId) {
        List<ConsentTemplateHeaderRow> rows = mapper.findVisibleVersionForUpdate(actorId, templateId, versionId);
        if (rows.size() != 1) {
            throw new IllegalArgumentException("동의서 템플릿을 찾을 수 없습니다.");
        }
        ConsentTemplateHeaderRow row = rows.get(0);
        Map<UUID, List<UUID>> selections = mapper.findSelections(versionId).stream()
                .collect(Collectors.groupingBy(ConsentDataSelectionRow::itemId,
                        Collectors.mapping(ConsentDataSelectionRow::processingDataItemId, Collectors.toList())));
        List<ConsentItemDefinition> items = mapper.findItems(versionId).stream()
                .map(item -> new ConsentItemDefinition(item.itemId(), item.itemOrder(), item.itemCode(),
                        item.itemType(), item.itemTitle(), item.required(), item.purposeText(),
                        item.retentionText(), item.refusalNotice(), item.thirdPartyRecipient(),
                        selections.getOrDefault(item.itemId(), List.of())))
                .toList();
        ConsentTemplateVersion version = new ConsentTemplateVersion(row.consentTemplateVersionId(),
                row.consentTemplateId(), row.processingActivityId(), row.processingActivityVersionId(),
                row.versionNumber(), row.workflowStatus(), row.title(), row.introductionHtml(), items,
                row.createdBy(), row.reviewRequestedBy(), row.approvedBy(), row.publishedBy(),
                row.rejectionComment(), row.contentHash(), row.versionNo());
        return new ConsentTemplateDetail(row.consentTemplateId(), row.templateCode(), row.templateName(),
                row.processingActivityId(), row.processingActivityName(), row.templateStatus(),
                row.currentPublishedVersionId(), row.templateVersionNo(), version);
    }

    @Override
    @Transactional
    public void replaceDraft(ConsentTemplateVersion draft, UUID actorId, Instant now) {
        mapper.clearDraftItems(draft.consentTemplateVersionId(), actorId);
        requireOne(mapper.updateDraft(draft, actorId, now), "다른 사용자가 초안을 변경했습니다.");
        insertItems(draft);
    }

    private void insertItems(ConsentTemplateVersion version) {
        mapper.insertItems(version.consentTemplateVersionId(), version.processingActivityVersionId(),
                version.items());
        List<ConsentDataSelectionInsert> selections = version.items().stream()
                .flatMap(item -> item.processingDataItemIds().stream()
                        .map(dataItemId -> new ConsentDataSelectionInsert(item.itemId(),
                                version.processingActivityVersionId(), dataItemId)))
                .toList();
        mapper.insertSelections(selections);
    }

    private void requireOne(int count, String message) {
        if (count != 1) {
            throw new IllegalStateException(message);
        }
    }
}
