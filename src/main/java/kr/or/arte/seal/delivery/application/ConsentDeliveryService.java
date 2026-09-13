package kr.or.arte.seal.delivery.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.link.application.IssuedSecureLink;
import kr.or.arte.seal.link.application.SecureLinkManagementService;
import kr.or.arte.seal.recipient.application.RecipientDeliveryContactQuery;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.PersonalDataContext;
import kr.or.arte.seal.request.application.ConsentRequestRecord;
import kr.or.arte.seal.request.application.ConsentRequestRepository;
import kr.or.arte.seal.request.application.ConsentRestrictionQuery;
import kr.or.arte.seal.request.application.RequestBusinessConflictException;
import kr.or.arte.seal.request.application.RequestNotFoundException;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ConsentDeliveryService {
    private final ConsentRequestRepository requests;
    private final SecureLinkManagementService links;
    private final RecipientDeliveryContactQuery contacts;
    private final ConsentMailRenderer renderer;
    private final PersonalDataCipher cipher;
    private final MailOutboxRepository outbox;
    private final AuditEventWriter audit;
    private final ConsentRestrictionQuery restrictions;
    private final Clock clock;

    public ConsentDeliveryService(ConsentRequestRepository requests,
            SecureLinkManagementService links, RecipientDeliveryContactQuery contacts,
            ConsentMailRenderer renderer, PersonalDataCipher cipher,
            MailOutboxRepository outbox, AuditEventWriter audit,
            ConsentRestrictionQuery restrictions, Clock clock) {
        this.requests = requests;
        this.links = links;
        this.contacts = contacts;
        this.renderer = renderer;
        this.cipher = cipher;
        this.outbox = outbox;
        this.audit = audit;
        this.restrictions = restrictions;
        this.clock = clock;
    }

    @Transactional
    public DeliveryIssueResult activateAndQueue(UUID actorId, UUID requestId,
            long versionNo, String baseUrl, Instant scheduledAt) {
        ConsentRequestRecord request = requests.findScopedForUpdate(actorId, requestId)
                .orElseThrow(RequestNotFoundException::new);
        if (request.status() != ConsentRequestStatus.READY) {
            throw new RequestBusinessConflictException("발송 준비 상태의 요청만 발송할 수 있습니다.");
        }
        requireNotRestricted(request.recipientId());
        IssuedSecureLink issued = links.activate(actorId, requestId, versionNo);
        return queue(actorId, request, issued, baseUrl, scheduledAt);
    }

    @Transactional
    public DeliveryIssueResult reissueAndQueue(UUID actorId, UUID requestId,
            long versionNo, String baseUrl, Instant scheduledAt) {
        ConsentRequestRecord request = requests.findScopedForUpdate(actorId, requestId)
                .orElseThrow(RequestNotFoundException::new);
        if (request.status() != ConsentRequestStatus.ACTIVE) {
            throw new RequestBusinessConflictException("진행 중인 요청만 재발송할 수 있습니다.");
        }
        requireNotRestricted(request.recipientId());
        outbox.cancelPending(requestId, clock.instant());
        IssuedSecureLink issued = links.reissue(actorId, requestId, versionNo);
        return queue(actorId, request, issued, baseUrl, scheduledAt);
    }

    private void requireNotRestricted(UUID recipientId) {
        LocalDate today = clock.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        if (restrictions.hasActiveRestriction(recipientId, today)) {
            throw new RequestBusinessConflictException("처리제한 중인 대상자에게 메일을 발송할 수 없습니다.");
        }
    }

    private DeliveryIssueResult queue(UUID actorId, ConsentRequestRecord request,
            IssuedSecureLink issued, String baseUrl, Instant scheduledAt) {
        var contact = contacts.find(actorId, request.recipientId());
        String snapshot = requests.findSnapshotJsonScoped(actorId, request.requestId())
                .orElseThrow(RequestNotFoundException::new);
        String url = baseUrl.replaceAll("/$", "") + "/consent/" + issued.token();
        var mail = renderer.render(snapshot, contact.name(), url);
        UUID outboxId = UUID.randomUUID();
        var email = cipher.encrypt(new PersonalDataContext("MAIL_OUTBOX", outboxId,
                request.organizationUnitId(), "recipientEmail"), contact.email());
        var html = cipher.encrypt(new PersonalDataContext("MAIL_OUTBOX", outboxId,
                request.organizationUnitId(), "html"), mail.html());
        Instant now = clock.instant();
        Instant sendAt = scheduledAt == null || scheduledAt.isBefore(now) ? now : scheduledAt;
        var row = new MailOutboxRecord(outboxId, request.requestId(),
                Objects.requireNonNull(issued.linkId()), request.organizationUnitId(),
                email.bytes(), html.bytes(), email.keyId(), mail.subject(), mail.sha256(),
                MailOutboxStatus.QUEUED, sendAt, sendAt, null, 0, null, 0);
        outbox.insert(row, actorId, now);
        audit.append(new AuditEventCommand(now, "ADMIN", actorId, "MAIL_OUTBOX_CREATE",
                "MAIL_OUTBOX", outboxId.toString(), "SUCCESS", null, UUID.randomUUID(),
                Map.of("requestId", request.requestId().toString(), "status", "QUEUED")));
        return new DeliveryIssueResult(outboxId, issued, MailOutboxStatus.QUEUED);
    }
}
