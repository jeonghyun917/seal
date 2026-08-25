package kr.or.arte.seal.request.application;

import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.delivery.application.ConsentDeliveryService;
import kr.or.arte.seal.delivery.application.ConsentMailRenderer;
import kr.or.arte.seal.delivery.application.MailOutboxRepository;
import kr.or.arte.seal.link.application.SecureLinkManagementService;
import kr.or.arte.seal.link.application.SecureLinkRepository;
import kr.or.arte.seal.recipient.application.RecipientDeliveryContactQuery;
import kr.or.arte.seal.recipient.application.RecipientRepository;
import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import kr.or.arte.seal.template.application.PublishedConsentTemplateQuery;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import java.time.ZoneId;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ConsentRestrictionEnforcementTest {
    private static final Instant NOW = Instant.parse("2026-08-24T15:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ORG = UUID.randomUUID();
    private static final UUID RECIPIENT = UUID.randomUUID();
    private static final UUID REQUEST = UUID.randomUUID();

    @Test
    void activeRestrictionBlocksNewRequestBeforeTemplateLookup() {
        ConsentRequestRepository requests = mock(ConsentRequestRepository.class);
        RecipientRepository recipients = mock(RecipientRepository.class);
        PublishedConsentTemplateQuery templates = mock(PublishedConsentTemplateQuery.class);
        ConsentRestrictionQuery retention = mock(ConsentRestrictionQuery.class);
        given(requests.findByIdempotency(ACTOR, ORG, UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .willReturn(Optional.empty());
        given(recipients.findScoped(ACTOR, RECIPIENT)).willReturn(Optional.of(recipient()));
        given(retention.hasActiveRestriction(RECIPIENT, NOW.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()))
                .willReturn(true);
        var service = new ConsentRequestManagementService(requests, recipients, templates,
                mock(RequestSnapshotFactory.class), mock(AuditEventWriter.class),
                mock(SecureLinkRepository.class), retention, (requestId, now) -> { }, CLOCK);

        var command = new CreateConsentRequestCommand(ORG, RECIPIENT, UUID.randomUUID(),
                NOW.plusSeconds(3600), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThatThrownBy(() -> service.create(ACTOR, command))
                .isInstanceOf(RequestBusinessConflictException.class)
                .hasMessageContaining("처리제한");
        verifyNoInteractions(templates);
    }

    @Test
    void activeRestrictionBlocksInitialDeliveryAndReissueBeforeLinkMutation() {
        ConsentRequestRepository requests = mock(ConsentRequestRepository.class);
        SecureLinkManagementService links = mock(SecureLinkManagementService.class);
        ConsentRestrictionQuery retention = mock(ConsentRestrictionQuery.class);
        given(retention.hasActiveRestriction(RECIPIENT, NOW.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()))
                .willReturn(true);
        var service = new ConsentDeliveryService(requests, links,
                mock(RecipientDeliveryContactQuery.class), mock(ConsentMailRenderer.class),
                mock(PersonalDataCipher.class), mock(MailOutboxRepository.class),
                mock(AuditEventWriter.class), retention, CLOCK);
        ConsentRequestRecord ready = request(ConsentRequestStatus.READY);
        ConsentRequestRecord active = request(ConsentRequestStatus.ACTIVE);
        given(requests.findScopedForUpdate(ACTOR, REQUEST)).willReturn(Optional.of(ready), Optional.of(active));

        assertThatThrownBy(() -> service.activateAndQueue(ACTOR, REQUEST, 0, "http://localhost:9090", null))
                .isInstanceOf(RequestBusinessConflictException.class)
                .hasMessageContaining("처리제한");
        assertThatThrownBy(() -> service.reissueAndQueue(ACTOR, REQUEST, 0, "http://localhost:9090", null))
                .isInstanceOf(RequestBusinessConflictException.class)
                .hasMessageContaining("처리제한");
        verifyNoInteractions(links);
    }

    private static RecipientStoredRecord recipient() {
        return new RecipientStoredRecord(RECIPIENT, ORG, new byte[30], new byte[30], "v1",
                "a".repeat(64), "교육 참여 신청서 수집", RecipientStatus.ACTIVE, 0);
    }

    private static ConsentRequestRecord request(ConsentRequestStatus status) {
        return new ConsentRequestRecord(REQUEST, "CR-20260825-ABCDEFGH", ORG, RECIPIENT,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), status, NOW.plusSeconds(3600), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), ACTOR, NOW, 0);
    }
}
