package kr.or.arte.seal.delivery.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.recipient.crypto.*;

import java.time.*;
import java.util.Map;
import java.util.UUID;

public class MailOutboxWorker {
    private final MailOutboxRepository repository;
    private final MailTransport transport;
    private final PersonalDataCipher cipher;
    private final boolean enabled;
    private final Clock clock;
    private final AuditEventWriter audit;

    public MailOutboxWorker(MailOutboxRepository repository, MailTransport transport,
            PersonalDataCipher cipher, boolean enabled, Clock clock) {
        this(repository, transport, cipher, enabled, clock, command -> { });
    }

    public MailOutboxWorker(MailOutboxRepository repository, MailTransport transport,
            PersonalDataCipher cipher, boolean enabled, Clock clock, AuditEventWriter audit) {
        this.repository = repository;
        this.transport = transport;
        this.cipher = cipher;
        this.enabled = enabled;
        this.clock = clock;
        this.audit = audit;
    }

    public int processBatch(int limit) {
        if (!enabled) return 0;
        Instant now = clock.instant();
        var rows = repository.claimDue(now, limit, now.minus(Duration.ofMinutes(10)));
        for (var row : rows) process(row, now);
        return rows.size();
    }

    private void process(MailOutboxRecord row, Instant now) {
        int attempt = row.attemptCount() + 1;
        if (!repository.isCurrentActiveLink(row.outboxId(), row.linkId(), now)) {
            repository.cancelClaim(row.outboxId(), row.versionNo(), now);
            audit(row, attempt, "MAIL_CANCELLED_STALE_LINK", "SUCCESS",
                    "LINK_REISSUED_OR_EXPIRED", now);
            return;
        }
        String email;
        String html;
        try {
            email = decrypt(row, "recipientEmail", row.emailCiphertext());
            html = decrypt(row, "html", row.htmlCiphertext());
        } catch (RuntimeException exception) {
            repository.failed(row.outboxId(), attempt, row.versionNo(), "OUTBOX_CONTENT_INVALID", now);
            audit(row, attempt, "MAIL_SEND_FAILED", "FAILURE", "OUTBOX_CONTENT_INVALID", now);
            return;
        }

        try {
            transport.send(new MailMessage(email, row.subject(), html, messageId(row.outboxId())));
        } catch (MailTransportException exception) {
            if (exception.temporary() && attempt < 4) {
                repository.retry(row.outboxId(), attempt, row.versionNo(), now.plus(delay(attempt)),
                        exception.reasonCode(), now);
                audit(row, attempt, "MAIL_RETRY_SCHEDULED", "FAILURE", exception.reasonCode(), now);
            } else {
                repository.failed(row.outboxId(), attempt, row.versionNo(), exception.reasonCode(), now);
                audit(row, attempt, "MAIL_SEND_FAILED", "FAILURE", exception.reasonCode(), now);
            }
            return;
        }

        repository.accepted(row.outboxId(), attempt, row.versionNo(), now);
        audit(row, attempt, "MAIL_SMTP_ACCEPTED", "SUCCESS", null, now);
    }

    private void audit(MailOutboxRecord row, int attempt, String action, String result,
            String reasonCode, Instant now) {
        audit.append(new AuditEventCommand(now, "SYSTEM", null, action, "MAIL_OUTBOX",
                row.outboxId().toString(), result, reasonCode, UUID.randomUUID(),
                Map.of("attempt", attempt, "requestId", row.requestId().toString())));
    }

    private String decrypt(MailOutboxRecord row, String field, byte[] bytes) {
        return cipher.decrypt(new PersonalDataContext("MAIL_OUTBOX", row.outboxId(), row.organizationId(), field),
                new EncryptedValue(row.keyId(), bytes));
    }

    static String messageId(UUID outboxId) {
        return "<seal-" + outboxId + "@arte.or.kr>";
    }

    private static Duration delay(int attempt) {
        return switch (attempt) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            default -> Duration.ofMinutes(30);
        };
    }
}
