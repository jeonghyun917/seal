package kr.or.arte.seal.delivery.application;

import kr.or.arte.seal.recipient.crypto.EncryptedValue;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.PersonalDataContext;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.time.Clock;

@Service
public class MailOutboxQueryService {
    private final MailOutboxRepository repository;
    private final PersonalDataCipher cipher;
    private final Clock clock;
    private final AuditEventWriter audit;

    public MailOutboxQueryService(MailOutboxRepository repository, PersonalDataCipher cipher, Clock clock,
            AuditEventWriter audit) {
        this.repository = repository;
        this.cipher = cipher;
        this.clock = clock;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<MailOutboxSummary> search(UUID actorId, MailOutboxStatus status) {
        return repository.search(actorId, status).stream()
                .map(row -> new MailOutboxSummary(row.outboxId(), row.requestId(), row.status(), row.subject(),
                        row.scheduledAt(), row.nextAttemptAt(), row.attemptCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public kr.or.arte.seal.common.web.AdminPageResult<MailOutboxSummary> searchPage(UUID actorId,
            MailOutboxStatus status,int page,int size){
        return searchPage(actorId, status, null, page, size);
    }

    @Transactional(readOnly = true)
    public kr.or.arte.seal.common.web.AdminPageResult<MailOutboxSummary> searchPage(UUID actorId,
            MailOutboxStatus status, String query, int page, int size){
        int safePage=Math.max(1,page),safeSize=Math.max(1,Math.min(100,size)),offset=(safePage-1)*safeSize;
        String normalized = query == null || query.isBlank() ? null : query.strip();
        var rows=repository.search(actorId,status,normalized,safeSize+1,offset).stream()
                .map(row -> new MailOutboxSummary(row.outboxId(),row.requestId(),row.status(),row.subject(),
                        row.scheduledAt(),row.nextAttemptAt(),row.attemptCount())).toList();
        return kr.or.arte.seal.common.web.AdminPageResult.of(rows,safePage,safeSize);
    }

    public MailOutboxDetail require(UUID actorId, UUID outboxId) {
        var row = repository.findScoped(actorId, outboxId)
                .orElseThrow(() -> new IllegalArgumentException("메일 발송 내역을 찾을 수 없습니다."));
        if (row.status() == MailOutboxStatus.DESTROYED) {
            return new MailOutboxDetail(row.outboxId(), row.requestId(), row.status(), row.subject(),
                    row.scheduledAt(), row.nextAttemptAt(), row.smtpAcceptedAt(), row.attemptCount(), null);
        }
        String html = cipher.decrypt(
                new PersonalDataContext("MAIL_OUTBOX", row.outboxId(), row.organizationId(), "html"),
                new EncryptedValue(row.keyId(), row.htmlCiphertext()));
        if (!sha256(html).equals(row.htmlSha256())) {
            throw new IllegalStateException("보관된 이메일 본문의 무결성 검증에 실패했습니다.");
        }
        return new MailOutboxDetail(row.outboxId(), row.requestId(), row.status(), row.subject(),
                row.scheduledAt(), row.nextAttemptAt(), row.smtpAcceptedAt(), row.attemptCount(), html);
    }

    @Transactional
    public void requeue(UUID actorId, UUID outboxId) {
        var now = clock.instant();
        repository.requeue(actorId, outboxId, now);
        audit.append(new AuditEventCommand(now, "ADMIN", actorId, "MAIL_OUTBOX_RETRY", "MAIL_OUTBOX",
                outboxId.toString(), "SUCCESS", null, UUID.randomUUID(), java.util.Map.of()));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
