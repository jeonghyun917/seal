package kr.or.arte.seal.template.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityQuery;
import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityReference;
import kr.or.arte.seal.template.domain.ConsentItemDefinition;
import kr.or.arte.seal.template.domain.ConsentItemType;
import kr.or.arte.seal.template.domain.ConsentTemplateVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ConsentTemplateManagementService implements ConsentTemplateManagement {
    private static final Pattern TEMPLATE_CODE = Pattern.compile("^[A-Z0-9-]{2,30}$");

    private final ConsentTemplateRepository repository;
    private final ApprovedProcessingActivityQuery policyQuery;
    private final TemplateHtmlSanitizer sanitizer;
    private final TemplateContentHasher hasher;
    private final AuditEventWriter auditWriter;
    private final Clock clock;
    private final TemplatePolicyValidator validator = new TemplatePolicyValidator();

    public ConsentTemplateManagementService(ConsentTemplateRepository repository,
            ApprovedProcessingActivityQuery policyQuery, TemplateHtmlSanitizer sanitizer,
            TemplateContentHasher hasher, AuditEventWriter auditWriter, Clock clock) {
        this.repository = repository;
        this.policyQuery = policyQuery;
        this.sanitizer = sanitizer;
        this.hasher = hasher;
        this.auditWriter = auditWriter;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UUID create(CreateConsentTemplateCommand command) {
        validateIdentity(command.templateCode(), command.templateName());
        Objects.requireNonNull(command.actorId(), "actorId");
        ApprovedProcessingActivityReference policy = policyQuery.findApprovedForTemplate(
                command.actorId(), command.processingActivityVersionId());
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ConsentTemplateVersion draft = ConsentTemplateVersion.draft(versionId, templateId,
                policy.activityId(), policy.versionId(), 1, command.title(),
                sanitizeIntroduction(command.introductionHtml()),
                toItems(command.items(), policy), command.actorId());
        validator.validateAgainst(draft, policy);
        repository.insertTemplate(new ConsentTemplateDetail(templateId, command.templateCode(),
                command.templateName().strip(), policy.activityId(), policy.activityName(),
                "ACTIVE", null, 0, draft), command.actorId(), clock.instant());
        audit("CONSENT_TEMPLATE_CREATE", templateId, versionId, command.actorId(), null);
        return templateId;
    }

    @Override
    @Transactional
    public void updateDraft(UpdateConsentTemplateDraftCommand command) {
        ConsentTemplateDetail detail = repository.requireVersionForUpdate(
                command.actorId(), command.templateId(), command.versionId());
        if (detail.templateVersionNo() != command.templateVersionNo()
                || detail.version().versionNo() != command.contentVersionNo()) {
            throw conflict("다른 사용자가 동의서를 변경했습니다.", null);
        }
        ApprovedProcessingActivityReference policy = policy(command.actorId(), detail);
        ConsentTemplateVersion changed = domain(() -> detail.version().changeContent(
                command.title(), sanitizeIntroduction(command.introductionHtml()),
                toItems(command.items(), policy)));
        validator.validateAgainst(changed, policy);
        validateName(command.templateName());
        repository.updateTemplateName(command.templateId(), command.templateName().strip(),
                command.templateVersionNo(), command.actorId(), clock.instant());
        repository.replaceDraft(changed, command.actorId(), clock.instant());
        audit("CONSENT_TEMPLATE_UPDATE", command.templateId(), command.versionId(),
                command.actorId(), null);
    }

    @Override
    @Transactional
    public void requestReview(UUID templateId, UUID versionId, UUID actorId) {
        ConsentTemplateDetail detail = find(actorId, templateId, versionId);
        validator.validateAgainst(detail.version(), policy(actorId, detail));
        ConsentTemplateVersion requested = domain(() -> detail.version().requestReview(actorId));
        repository.updateTransition(requested, actorId, "REQUESTED", null, clock.instant());
        audit("CONSENT_TEMPLATE_REVIEW_REQUEST", templateId, versionId, actorId, null);
    }

    @Override
    @Transactional
    public void approve(UUID templateId, UUID versionId, UUID actorId) {
        ConsentTemplateDetail detail = find(actorId, templateId, versionId);
        validator.validateAgainst(detail.version(), policy(actorId, detail));
        ConsentTemplateVersion approved = domain(() -> detail.version().approve(actorId));
        repository.updateTransition(approved, actorId, "APPROVED", null, clock.instant());
        audit("CONSENT_TEMPLATE_APPROVE", templateId, versionId, actorId, null);
    }

    @Override
    @Transactional
    public void reject(UUID templateId, UUID versionId, UUID actorId, String comment) {
        ConsentTemplateDetail detail = find(actorId, templateId, versionId);
        ConsentTemplateVersion rejected = domain(() -> detail.version().reject(actorId, comment));
        repository.updateTransition(rejected, actorId, "REJECTED", comment, clock.instant());
        audit("CONSENT_TEMPLATE_REJECT", templateId, versionId, actorId, null);
    }

    @Override
    @Transactional
    public void publish(UUID templateId, UUID versionId, UUID actorId) {
        ConsentTemplateDetail detail = find(actorId, templateId, versionId);
        ApprovedProcessingActivityReference policy = policy(actorId, detail);
        validator.validateAgainst(detail.version(), policy);
        String hash = hasher.sha256(Map.of("templateCode", detail.templateCode(),
                "version", detail.version(), "policy", policy));
        ConsentTemplateVersion published = domain(() -> detail.version().publish(actorId, hash));
        repository.publish(published, actorId, clock.instant());
        audit("CONSENT_TEMPLATE_PUBLISH", templateId, versionId, actorId, hash);
    }

    @Override
    @Transactional
    public UUID createNextDraft(UUID templateId, UUID sourceVersionId, UUID actorId) {
        ConsentTemplateDetail source = find(actorId, templateId, sourceVersionId);
        if (!repository.isLatestPublishedOrRetired(templateId, sourceVersionId)) {
            throw new TemplateBusinessConflictException("가장 최근 발행 또는 종료 버전만 복제할 수 있습니다.");
        }
        policy(actorId, source);
        UUID newVersionId = UUID.randomUUID();
        List<ConsentItemDefinition> copiedItems = source.version().items().stream()
                .map(item -> new ConsentItemDefinition(UUID.randomUUID(), item.order(), item.code(),
                        item.type(), item.title(), item.required(), item.purpose(), item.retentionText(),
                        item.refusalNotice(), item.thirdPartyRecipient(), item.processingDataItemIds()))
                .toList();
        ConsentTemplateVersion draft = ConsentTemplateVersion.draft(newVersionId, templateId,
                source.version().processingActivityId(), source.version().processingActivityVersionId(),
                repository.nextVersionNumber(templateId), source.version().title(),
                source.version().introductionHtml(), copiedItems, actorId);
        repository.insertVersion(draft, actorId, clock.instant());
        audit("CONSENT_TEMPLATE_VERSION_CREATE", templateId, newVersionId, actorId, null);
        return newVersionId;
    }

    @Override
    @Transactional
    public void updateStatus(UUID templateId, String status, long versionNo, UUID actorId) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new IllegalArgumentException("템플릿 상태가 올바르지 않습니다.");
        }
        repository.updateStatus(templateId, status, versionNo, actorId, clock.instant());
        audit("CONSENT_TEMPLATE_STATUS_UPDATE", templateId, templateId, actorId, null);
    }

    private ConsentTemplateDetail find(UUID actorId, UUID templateId, UUID versionId) {
        try {
            ConsentTemplateDetail detail = repository.requireVersionForUpdate(actorId, templateId, versionId);
            if (!"ACTIVE".equals(detail.templateStatus())) {
                throw new TemplateBusinessConflictException("비활성 템플릿은 처리할 수 없습니다.");
            }
            return detail;
        } catch (IllegalArgumentException exception) {
            throw new TemplateNotFoundException();
        }
    }

    private ApprovedProcessingActivityReference policy(UUID actorId, ConsentTemplateDetail detail) {
        try {
            return policyQuery.findApprovedForTemplate(actorId,
                    detail.version().processingActivityVersionId());
        } catch (IllegalArgumentException exception) {
            throw conflict("승인 처리업무 버전이 더 이상 유효하지 않습니다.", exception);
        }
    }

    private List<ConsentItemDefinition> toItems(List<ConsentItemDraftCommand> commands,
            ApprovedProcessingActivityReference policy) {
        if (commands == null) {
            throw new IllegalArgumentException("동의 항목을 하나 이상 입력해야 합니다.");
        }
        return java.util.stream.IntStream.range(0, commands.size()).mapToObj(index -> {
            ConsentItemDraftCommand item = commands.get(index);
            String retention = item.type() == ConsentItemType.THIRD_PARTY_PROVISION
                    ? item.retentionText() : policy.retentionDisplayText();
            return new ConsentItemDefinition(UUID.randomUUID(), index + 1, item.code(), item.type(),
                    item.title(), item.required(), item.purpose(), retention, item.refusalNotice(),
                    item.thirdPartyRecipient(), item.processingDataItemIds());
        }).toList();
    }

    private String sanitizeIntroduction(String html) {
        String sanitized = sanitizer.sanitize(html);
        String text = sanitized.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").strip();
        if (text.length() < 10) {
            throw new IllegalArgumentException("동의서 안내문 내용은 10자 이상이어야 합니다.");
        }
        return sanitized;
    }

    private static void validateIdentity(String code, String name) {
        if (code == null || !TEMPLATE_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("템플릿 코드는 영문 대문자, 숫자, 하이픈 2~30자로 입력해야 합니다.");
        }
        validateName(name);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new IllegalArgumentException("템플릿 명칭은 1~100자로 입력해야 합니다.");
        }
    }

    private ConsentTemplateVersion domain(DomainOperation operation) {
        try {
            return operation.apply();
        } catch (IllegalStateException exception) {
            throw conflict(exception.getMessage(), exception);
        }
    }

    private TemplateBusinessConflictException conflict(String message, Throwable cause) {
        return cause == null ? new TemplateBusinessConflictException(message)
                : new TemplateBusinessConflictException(message, cause);
    }

    private void audit(String action, UUID templateId, UUID versionId, UUID actorId, String hash) {
        Map<String, Object> metadata = hash == null
                ? Map.of("versionId", versionId.toString())
                : Map.of("versionId", versionId.toString(), "contentHash", hash);
        auditWriter.append(new AuditEventCommand(clock.instant(), "ADMIN", actorId, action,
                "CONSENT_TEMPLATE", templateId.toString(), "SUCCESS", null,
                UUID.randomUUID(), metadata));
    }

    @FunctionalInterface
    private interface DomainOperation {
        ConsentTemplateVersion apply();
    }
}
