package kr.or.arte.seal.retention.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.evidence.infrastructure.LocalEvidenceStorage;
import kr.or.arte.seal.retention.domain.RetentionAnchorType;
import kr.or.arte.seal.retention.domain.RetentionJobStatus;
import kr.or.arte.seal.retention.domain.RetentionJobTransition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class RetentionJobService implements RetentionJobRequestPort {
    private final RetentionRepository repository;
    private final LocalEvidenceStorage storage;
    private final Clock clock;
    private final AuditEventWriter audit;

    public RetentionJobService(RetentionRepository repository,
                               @Value("${seal.evidence.root}") String root,
                               Clock clock,
                               AuditEventWriter audit) {
        this.repository = repository;
        this.storage = new LocalEvidenceStorage(Path.of(root));
        this.clock = clock;
        this.audit = audit;
    }

    public UUID requestUrgent(UUID rights, UUID request, UUID recipient, UUID actor, Instant now) {
        if (request == null) throw new IllegalArgumentException("삭제 요청에는 동의요청 연결이 필요합니다.");
        return repository.requestUrgent(UUID.randomUUID(), rights, request, recipient, actor, now);
    }

    public boolean isConfirmed(UUID jobId, UUID actor) {
        return require(actor, jobId).status() == RetentionJobStatus.CONFIRMED;
    }

    public void placeRestrictionHold(UUID request, UUID actor, Instant now) {
        if (request == null) throw new IllegalArgumentException("처리제한에는 동의요청 연결이 필요합니다.");
        repository.placeRestrictionHold(UUID.randomUUID(), request, actor, now);
    }

    @Transactional
    public void recordAnchor(UUID actor, UUID request, RetentionAnchorType type, LocalDate anchorDate) {
        Objects.requireNonNull(type, "기산일 유형이 필요합니다.");
        Objects.requireNonNull(anchorDate, "기산일이 필요합니다.");
        Instant now = clock.instant();
        repository.recordAnchor(request, type, anchorDate, actor, now);
        audit.append(audit(now, actor, "RETENTION_ANCHOR_RECORD", "CONSENT_REQUEST", request,
                Map.of("anchorType", type.name(), "anchorDate", anchorDate.toString())));
    }

    @Transactional
    public void placeLegalHold(UUID actor, UUID request, String reasonCode, String reasonText,
                               LocalDate expiresOn) {
        String code = requireText(reasonCode, "보존 보류 사유 코드가 필요합니다.", 100);
        String text = requireText(reasonText, "보존 보류 근거가 필요합니다.", 1000);
        if (expiresOn != null && expiresOn.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("보존 보류 만료일은 오늘보다 빠를 수 없습니다.");
        }
        Instant now = clock.instant();
        repository.placeLegalHold(request, code, text, expiresOn, actor, now);
        audit.append(audit(now, actor, "LEGAL_HOLD_PLACE", "CONSENT_REQUEST", request,
                Map.of("reasonCode", code, "hasExpiry", expiresOn != null)));
    }

    @Transactional
    public void releaseLegalHold(UUID actor, UUID holdId) {
        Instant now = clock.instant();
        repository.releaseLegalHold(holdId, actor, now);
        audit.append(audit(now, actor, "LEGAL_HOLD_RELEASE", "LEGAL_HOLD", holdId, Map.of()));
    }

    public List<ActiveLegalHoldSummary> activeLegalHolds(UUID actor) {
        return repository.activeLegalHolds(actor);
    }

    public int scheduleDue() {
        return repository.scheduleDue(LocalDate.now(clock), clock.instant());
    }

    public List<RetentionJobSummary> search(UUID actor, RetentionJobStatus status) {
        return repository.search(actor, status);
    }

    public RetentionJobRecord require(UUID actor, UUID id) {
        return repository.findScoped(actor, id)
                .orElseThrow(() -> new NoSuchElementException("파기 작업을 찾을 수 없습니다."));
    }

    @Transactional
    public void submit(UUID actor, UUID id) {
        move(actor, id, RetentionJobStatus.PENDING_APPROVAL, null, null);
    }

    @Transactional
    public void approve(UUID actor, UUID id) {
        var record = lock(actor, id);
        RetentionJobTransition.requireDifferent(record.requestedBy(), actor, "APPROVER");
        RetentionJobTransition.requireNoHold(repository.hasActiveHold(record.consentRequestId(), LocalDate.now(clock)));
        moveLocked(record, actor, RetentionJobStatus.APPROVED, null, null);
    }

    @Transactional
    public RetentionExecutionResult execute(UUID actor, UUID id, UUID key) {
        var record = lock(actor, id);
        if (record.status() == RetentionJobStatus.EXECUTED || record.status() == RetentionJobStatus.CONFIRMED) {
            if (Objects.equals(key, record.idempotencyKey())) {
                return new RetentionExecutionResult(record.destroyedMailCount(), record.destroyedFileCount(),
                        record.recipientPiiDestroyed());
            }
            throw new IllegalStateException("이미 다른 멱등키로 실행된 작업입니다.");
        }
        RetentionJobTransition.next(record.status(), RetentionJobStatus.EXECUTING);
        repository.transition(id, record.status(), RetentionJobStatus.EXECUTING, actor, key, null,
                clock.instant(), record.rowVersion());
        int files = 0;
        for (String path : repository.evidencePaths(record.consentRequestId())) {
            files += storage.deleteDirectory(path);
        }
        RetentionExecutionResult result = repository.destroyPrimaryData(id, record.consentRequestId(),
                record.recipientId(), files, clock.instant());
        repository.transition(id, RetentionJobStatus.EXECUTING, RetentionJobStatus.EXECUTED, actor, key,
                result, clock.instant(), record.rowVersion() + 1);
        repository.event(id, record.status(), RetentionJobStatus.EXECUTED, actor, null, clock.instant());
        return result;
    }

    @Transactional
    public void confirm(UUID actor, UUID id) {
        var record = lock(actor, id);
        RetentionJobTransition.requireDifferent(record.executedBy(), actor, "CONFIRMER");
        moveLocked(record, actor, RetentionJobStatus.CONFIRMED, null, null);
    }

    private void move(UUID actor, UUID id, RetentionJobStatus to, UUID key, RetentionExecutionResult result) {
        moveLocked(lock(actor, id), actor, to, key, result);
    }

    private void moveLocked(RetentionJobRecord record, UUID actor, RetentionJobStatus to, UUID key,
                            RetentionExecutionResult result) {
        RetentionJobStatus changed = RetentionJobTransition.next(record.status(), to);
        repository.transition(record.id(), record.status(), changed, actor, key, result, clock.instant(),
                record.rowVersion());
        repository.event(record.id(), record.status(), changed, actor, null, clock.instant());
    }

    private RetentionJobRecord lock(UUID actor, UUID id) {
        return repository.findScopedForUpdate(actor, id)
                .orElseThrow(() -> new NoSuchElementException("파기 작업을 찾을 수 없습니다."));
    }

    private static String requireText(String value, String message, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException("입력값이 허용 길이를 초과했습니다.");
        return trimmed;
    }

    private static AuditEventCommand audit(Instant now, UUID actor, String action, String targetType,
                                           UUID targetId, Map<String, Object> metadata) {
        return new AuditEventCommand(now, "ADMIN", actor, action, targetType, targetId.toString(),
                "SUCCESS", null, UUID.randomUUID(), metadata);
    }
}
