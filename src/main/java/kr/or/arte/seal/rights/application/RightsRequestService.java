package kr.or.arte.seal.rights.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.recipient.crypto.*;
import kr.or.arte.seal.retention.application.RetentionJobRequestPort;
import kr.or.arte.seal.rights.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RightsRequestService {
    private final RightsRequestRepository repository;
    private final PersonalDataCipher cipher;
    private final RetentionJobRequestPort retention;
    private final Clock clock;
    private final AuditEventWriter audit;

    public RightsRequestService(RightsRequestRepository repository, PersonalDataCipher cipher,
            RetentionJobRequestPort retention, Clock clock) {
        this(repository, cipher, retention, clock, command -> { });
    }

    @Autowired
    public RightsRequestService(RightsRequestRepository repository, PersonalDataCipher cipher,
            RetentionJobRequestPort retention, Clock clock, AuditEventWriter audit) {
        this.repository = repository;
        this.cipher = cipher;
        this.retention = retention;
        this.clock = clock;
        this.audit = audit;
    }

    @Transactional
    public UUID create(UUID org, UUID recipient, UUID consentRequest, RightsRequestType type,
            LocalDate received, LocalDate due, String detail, UUID actor) {
        Objects.requireNonNull(org, "조직이 필요합니다.");
        Objects.requireNonNull(recipient, "대상자가 필요합니다.");
        Objects.requireNonNull(type, "권리요청 유형이 필요합니다.");
        Objects.requireNonNull(received, "접수일이 필요합니다.");
        Objects.requireNonNull(due, "처리기한이 필요합니다.");
        if (Set.of(RightsRequestType.ERASURE, RightsRequestType.RESTRICTION, RightsRequestType.WITHDRAWAL)
                .contains(type) && consentRequest == null) {
            throw new IllegalArgumentException("이 권리요청 유형에는 동의요청 연결이 필요합니다.");
        }
        if (due.isBefore(received)) throw new IllegalArgumentException("처리기한은 접수일보다 빠를 수 없습니다.");
        UUID id = UUID.randomUUID();
        EncryptedValue value = encrypt(id, org, "detail", detail);
        String code = "RR-" + received.format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + id.toString().substring(0, 8).toUpperCase();
        Instant now = clock.instant();
        repository.insert(new RightsRequestRecord(id, code, org, recipient, consentRequest, type,
                RightsRequestStatus.RECEIVED, received, due, value.bytes(), null, null, value.keyId(),
                null, null, null, actor, now, null, null, 0), actor);
        repository.insertEvent(id, null, RightsRequestStatus.RECEIVED, actor, null, now);
        audit(actor, id, "RIGHTS_REQUEST_CREATE", "RECEIVED", now);
        return id;
    }

    public List<RightsRequestSummary> search(UUID actor, RightsRequestStatus status) {
        return repository.search(actor, status);
    }

    public kr.or.arte.seal.common.web.AdminPageResult<RightsRequestSummary> searchPage(UUID actor,
            RightsRequestStatus status,int page,int size){
        int safePage=Math.max(1,page),safeSize=Math.max(1,Math.min(100,size)),offset=(safePage-1)*safeSize;
        return kr.or.arte.seal.common.web.AdminPageResult.of(
                repository.search(actor,status,safeSize+1,offset),safePage,safeSize);
    }

    public RightsRequestDetail require(UUID actor, UUID id) {
        var record = repository.findScoped(actor, id)
                .orElseThrow(() -> new NoSuchElementException("권리요청을 찾을 수 없습니다."));
        String detail = record.detailCiphertext() == null
                ? "보유기간 만료로 암호화 상세정보가 파기되었습니다."
                : decrypt(record, "detail", record.detailCiphertext(), record.keyId());
        return new RightsRequestDetail(record, detail,
                decryptNullable(record, "verificationNote", record.verificationNoteCiphertext(),
                        record.verificationNoteKeyId()),
                decryptNullable(record, "resultNote", record.resultNoteCiphertext(), record.resultNoteKeyId()));
    }

    @Transactional
    public void verifyIdentity(UUID actor, UUID id, String channel, String note) {
        requireText(channel, 50, "본인확인 경로");
        var record = lock(actor, id);
        RightsRequestStatus to = RightsRequestTransition.next(record.status(), RightsRequestStatus.IDENTITY_VERIFIED);
        EncryptedValue encrypted = encrypt(id, record.organizationId(), "verificationNote", note);
        Instant now = clock.instant();
        repository.transition(id, record.status(), to, actor, channel, encrypted.bytes(), encrypted.keyId(),
                null, null, null, now, record.rowVersion());
        repository.insertEvent(id, record.status(), to, actor, null, now);
        audit(actor, id, "RIGHTS_IDENTITY_VERIFY", to.name(), now);
    }

    @Transactional
    public void startReview(UUID actor, UUID id) {
        move(actor, id, RightsRequestStatus.IN_REVIEW, null, null, "RIGHTS_REVIEW_START");
    }

    @Transactional
    public void decide(UUID actor, UUID id, boolean approve, String reason) {
        requireText(reason, 1000, "결정 사유");
        var record = lock(actor, id);
        RightsRequestTransition.requireSeparateDecision(record.createdBy(), actor);
        RightsRequestStatus to = RightsRequestTransition.next(record.status(),
                approve ? RightsRequestStatus.APPROVED : RightsRequestStatus.REJECTED);
        Instant now = clock.instant();
        repository.transition(id, record.status(), to, actor, null, null, null, reason,
                null, null, now, record.rowVersion());
        repository.insertEvent(id, record.status(), to, actor, reason, now);
        audit(actor, id, approve ? "RIGHTS_APPROVE" : "RIGHTS_REJECT", to.name(), now);
    }

    @Transactional
    public void complete(UUID actor, UUID id, String result) {
        requireText(result, 2000, "처리 결과");
        var record = lock(actor, id);
        boolean approved = record.status() == RightsRequestStatus.APPROVED;
        if (approved && record.type() == RightsRequestType.ERASURE) {
            if (record.retentionJobId() == null) {
                UUID job = retention.requestUrgent(id, record.consentRequestId(), record.recipientId(), actor,
                        clock.instant());
                repository.linkRetentionJob(id, job, record.rowVersion());
                audit(actor, id, "RIGHTS_ERASURE_JOB_REQUEST", "PENDING_RETENTION", clock.instant());
                return;
            }
            if (!retention.isConfirmed(record.retentionJobId(), actor)) {
                throw new IllegalStateException("삭제 작업 확인이 완료된 뒤 권리요청을 완료할 수 있습니다.");
            }
        }
        RightsRequestStatus to = RightsRequestTransition.next(record.status(), RightsRequestStatus.COMPLETED);
        if (approved && record.type() == RightsRequestType.WITHDRAWAL) {
            UUID response = repository.responseId(record.consentRequestId())
                    .orElseThrow(() -> new IllegalStateException("완료 동의 응답이 없습니다."));
            repository.insertWithdrawal(id, response, actor, clock.instant());
        } else if (approved && record.type() == RightsRequestType.RESTRICTION) {
            retention.placeRestrictionHold(record.consentRequestId(), actor, clock.instant());
        }
        EncryptedValue encrypted = record.detailCiphertext() == null ? null
                : encrypt(id, record.organizationId(), "resultNote", result);
        Instant now = clock.instant();
        repository.transition(id, record.status(), to, actor, null, null, null, null,
                encrypted == null ? null : encrypted.bytes(), encrypted == null ? null : encrypted.keyId(),
                now, record.rowVersion());
        repository.insertEvent(id, record.status(), to, actor, null, now);
        audit(actor, id, "RIGHTS_COMPLETE", to.name(), now);
    }

    private void move(UUID actor, UUID id, RightsRequestStatus to, String reason, byte[] result, String action) {
        var record = lock(actor, id);
        RightsRequestStatus changed = RightsRequestTransition.next(record.status(), to);
        Instant now = clock.instant();
        repository.transition(id, record.status(), changed, actor, null, null, null, reason,
                result, null, now, record.rowVersion());
        repository.insertEvent(id, record.status(), changed, actor, reason, now);
        audit(actor, id, action, changed.name(), now);
    }

    private RightsRequestRecord lock(UUID actor, UUID id) {
        return repository.findScopedForUpdate(actor, id)
                .orElseThrow(() -> new NoSuchElementException("권리요청을 찾을 수 없습니다."));
    }

    private EncryptedValue encrypt(UUID id, UUID org, String field, String text) {
        requireText(text, 2000, "상세 내용");
        return cipher.encrypt(new PersonalDataContext("RIGHTS_REQUEST", id, org, field), text.strip());
    }

    private static void requireText(String text, int max, String label) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException(label + "이(가) 필요합니다.");
        if (text.length() > max) throw new IllegalArgumentException(label + "은(는) " + max + "자 이하여야 합니다.");
    }

    private String decrypt(RightsRequestRecord record, String field, byte[] value, String keyId) {
        return cipher.decrypt(new PersonalDataContext("RIGHTS_REQUEST", record.id(), record.organizationId(), field),
                new EncryptedValue(keyId, value));
    }

    private String decryptNullable(RightsRequestRecord record, String field, byte[] value, String keyId) {
        return value == null ? null : decrypt(record, field, value, keyId);
    }

    private void audit(UUID actor, UUID id, String action, String status, Instant now) {
        audit.append(new AuditEventCommand(now, "ADMIN", actor, action, "RIGHTS_REQUEST", id.toString(),
                "SUCCESS", null, UUID.randomUUID(), Map.of("status", status)));
    }
}
