package kr.or.arte.seal.request.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.recipient.application.RecipientRepository;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import kr.or.arte.seal.request.domain.ConsentRequest;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import kr.or.arte.seal.template.application.PublishedConsentTemplateQuery;
import kr.or.arte.seal.template.application.PublishedConsentTemplateReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class ConsentRequestManagementService implements ConsentRequestManagement {
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.of("Asia/Seoul"));
    private final ConsentRequestRepository repository;
    private final RecipientRepository recipients;
    private final PublishedConsentTemplateQuery templates;
    private final RequestSnapshotFactory snapshots;
    private final AuditEventWriter auditWriter;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public ConsentRequestManagementService(ConsentRequestRepository repository,
            RecipientRepository recipients, PublishedConsentTemplateQuery templates,
            RequestSnapshotFactory snapshots, AuditEventWriter auditWriter, Clock clock) {
        this.repository = repository; this.recipients = recipients; this.templates = templates;
        this.snapshots = snapshots; this.auditWriter = auditWriter; this.clock = clock;
    }

    @Override
    @Transactional
    public ConsentRequestCreateResult create(UUID actorId, CreateConsentRequestCommand command) {
        require(command.organizationUnitId(), "조직"); require(command.recipientId(), "대상자");
        require(command.templateVersionId(), "템플릿 버전"); require(command.idempotencyKey(), "멱등키");
        if (command.expiresAt() == null || !command.expiresAt().isAfter(clock.instant())) {
            throw new IllegalArgumentException("만료일시는 현재 이후여야 합니다.");
        }
        var existing = repository.findByIdempotency(actorId, command.organizationUnitId(), command.idempotencyKey());
        if (existing.isPresent()) return result(existing.get(), true);
        var recipient = recipients.findScoped(actorId, command.recipientId()).orElseThrow(RequestNotFoundException::new);
        if (!recipient.organizationUnitId().equals(command.organizationUnitId())
                || recipient.status() != RecipientStatus.ACTIVE) throw new RequestNotFoundException();
        PublishedConsentTemplateReference template = templates.requirePublished(actorId, command.templateVersionId());
        ConsentRequestRecord row = new ConsentRequestRecord(UUID.randomUUID(), requestCode(),
                command.organizationUnitId(), command.recipientId(), template.processingActivityId(),
                template.processingActivityVersionId(), template.templateId(), template.templateVersionId(),
                template.retentionPolicyVersionId(), ConsentRequestStatus.DRAFT, command.expiresAt(),
                command.idempotencyKey(), null, null, actorId, clock.instant(), 0);
        try {
            repository.insert(row, actorId, clock.instant());
        } catch (DataIntegrityViolationException exception) {
            return repository.findByIdempotency(actorId, command.organizationUnitId(), command.idempotencyKey())
                    .map(found -> result(found, true)).orElseThrow(() -> exception);
        }
        repository.insertEvent(row.requestId(), "CREATED", "ADMIN", actorId, null, clock.instant());
        audit("CONSENT_REQUEST_CREATE", row, actorId, null);
        return result(row, false);
    }

    @Override
    @Transactional
    public void finalizeRequest(UUID actorId, UUID requestId, long versionNo) {
        ConsentRequestRecord row = requireVersion(actorId, requestId, versionNo);
        PublishedConsentTemplateReference template = templates.requirePublished(actorId, row.templateVersionId());
        if (!template.templateId().equals(row.templateId())
                || !template.processingActivityVersionId().equals(row.processingActivityVersionId())
                || !template.retentionPolicyVersionId().equals(row.retentionPolicyVersionId())) {
            throw new RequestBusinessConflictException("요청 기준정보가 변경되어 확정할 수 없습니다.");
        }
        UUID snapshotId = UUID.randomUUID();
        ConsentRequest draft = ConsentRequest.draft(row.requestId(), row.requestCode(),
                row.organizationUnitId(), row.recipientId(), row.templateVersionId(),
                row.createdBy(), row.expiresAt());
        draft.finalizeAt(clock.instant(), snapshotId);
        RequestSnapshot snapshot = snapshots.create(new RequestSnapshotSource(
                "한국문화예술교육진흥원", row.requestCode(), row.processingActivityVersionId(),
                row.retentionPolicyVersionId(), template.retentionDisplayText(),
                row.templateVersionId(), template.contentHash(), row.expiresAt(),
                "동의 철회는 담당 부서에 요청할 수 있습니다.", template.view(),
                template.dataItemIdsByItemOrder()));
        repository.insertSnapshot(requestId, snapshotId, snapshot, actorId, clock.instant());
        if (!repository.markReady(requestId, snapshotId, actorId, clock.instant(), versionNo)) throw conflict();
        repository.insertEvent(requestId, "FINALIZED", "ADMIN", actorId, null, clock.instant());
        audit("CONSENT_REQUEST_FINALIZE", row, actorId, null);
    }

    @Override
    @Transactional
    public void cancel(UUID actorId, UUID requestId, long versionNo, String reason) {
        if (reason == null || reason.isBlank() || reason.strip().length() < 5 || reason.strip().length() > 500) {
            throw new IllegalArgumentException("취소 사유는 5~500자로 입력해 주세요.");
        }
        ConsentRequestRecord row = requireVersion(actorId, requestId, versionNo);
        new ConsentRequest(row.requestId(), row.requestCode(), row.organizationUnitId(), row.recipientId(),
                row.templateVersionId(), row.createdBy(), row.status(), row.expiresAt(), row.snapshotId(),
                row.currentSecureLinkId(), row.versionNo()).cancel();
        if (!repository.cancel(requestId, actorId, clock.instant(), reason.strip(), versionNo)) throw conflict();
        repository.insertEvent(requestId, "CANCELLED", "ADMIN", actorId, "ADMIN_CANCEL", clock.instant());
        audit("CONSENT_REQUEST_CANCEL", row, actorId, "ADMIN_CANCEL");
    }

    private ConsentRequestRecord requireVersion(UUID actorId, UUID id, long version) {
        ConsentRequestRecord row = repository.findScopedForUpdate(actorId, id)
                .orElseThrow(RequestNotFoundException::new);
        if (row.versionNo() != version) throw conflict();
        return row;
    }

    private String requestCode() {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) suffix.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
        return "CR-" + DATE.format(clock.instant()) + "-" + suffix;
    }

    private void audit(String action, ConsentRequestRecord row, UUID actorId, String reason) {
        auditWriter.append(new AuditEventCommand(clock.instant(), "ADMIN", actorId, action,
                "CONSENT_REQUEST", row.requestId().toString(), "SUCCESS", reason,
                UUID.randomUUID(), Map.of("organizationUnitId", row.organizationUnitId().toString(),
                        "requestStatus", row.status().name())));
    }

    private static ConsentRequestCreateResult result(ConsentRequestRecord row, boolean existing) {
        return new ConsentRequestCreateResult(row.requestId(), row.requestCode(), existing);
    }
    private static void require(Object value, String label) {
        if (value == null) throw new IllegalArgumentException(label + " 값이 필요합니다.");
    }
    private static RequestBusinessConflictException conflict() {
        return new RequestBusinessConflictException("다른 변경과 충돌했습니다. 최신 내용을 다시 불러오세요.");
    }
}
