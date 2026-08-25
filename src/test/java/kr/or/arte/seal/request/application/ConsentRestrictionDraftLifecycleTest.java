package kr.or.arte.seal.request.application;

import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.link.application.SecureLinkRepository;
import kr.or.arte.seal.recipient.application.RecipientRepository;
import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import kr.or.arte.seal.template.application.PublishedConsentTemplateQuery;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ConsentRestrictionDraftLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-08-24T15:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ORG = UUID.randomUUID();
    private static final UUID RECIPIENT = UUID.randomUUID();
    private static final UUID REQUEST = UUID.randomUUID();
    private static final UUID TEMPLATE_VERSION = UUID.randomUUID();

    @Test
    void activeRestrictionBlocksDraftRecipientChangeBeforeTemplateLookup() {
        ConsentRequestRepository requests = mock(ConsentRequestRepository.class);
        RecipientRepository recipients = mock(RecipientRepository.class);
        PublishedConsentTemplateQuery templates = mock(PublishedConsentTemplateQuery.class);
        ConsentRestrictionQuery restrictions = mock(ConsentRestrictionQuery.class);
        given(requests.findScopedForUpdate(ACTOR, REQUEST)).willReturn(Optional.of(draft()));
        given(recipients.findScoped(ACTOR, RECIPIENT)).willReturn(Optional.of(recipient()));
        given(restrictions.hasActiveRestriction(eq(RECIPIENT), eq(LocalDate.of(2026, 8, 25))))
                .willReturn(true);
        var service = service(requests, recipients, templates,
                mock(RequestSnapshotFactory.class), restrictions);

        assertThatThrownBy(() -> service.updateDraft(ACTOR, REQUEST,
                new UpdateConsentRequestCommand(RECIPIENT, TEMPLATE_VERSION,
                        NOW.plusSeconds(3600), 0)))
                .isInstanceOf(RequestBusinessConflictException.class)
                .hasMessageContaining("처리제한");
        verifyNoInteractions(templates);
    }

    @Test
    void activeRestrictionBlocksFinalizationBeforeSnapshotCreation() {
        ConsentRequestRepository requests = mock(ConsentRequestRepository.class);
        RecipientRepository recipients = mock(RecipientRepository.class);
        PublishedConsentTemplateQuery templates = mock(PublishedConsentTemplateQuery.class);
        RequestSnapshotFactory snapshots = mock(RequestSnapshotFactory.class);
        ConsentRestrictionQuery restrictions = mock(ConsentRestrictionQuery.class);
        given(requests.findScopedForUpdate(ACTOR, REQUEST)).willReturn(Optional.of(draft()));
        given(restrictions.hasActiveRestriction(eq(RECIPIENT), eq(LocalDate.of(2026, 8, 25))))
                .willReturn(true);
        var service = service(requests, recipients, templates, snapshots, restrictions);

        assertThatThrownBy(() -> service.finalizeRequest(ACTOR, REQUEST, 0))
                .isInstanceOf(RequestBusinessConflictException.class)
                .hasMessageContaining("처리제한");
        verifyNoInteractions(templates, snapshots);
    }

    private static ConsentRequestManagementService service(ConsentRequestRepository requests,
            RecipientRepository recipients, PublishedConsentTemplateQuery templates,
            RequestSnapshotFactory snapshots, ConsentRestrictionQuery restrictions) {
        return new ConsentRequestManagementService(requests, recipients, templates, snapshots,
                mock(AuditEventWriter.class), mock(SecureLinkRepository.class), restrictions,
                (requestId, now) -> { }, CLOCK);
    }

    private static RecipientStoredRecord recipient() {
        return new RecipientStoredRecord(RECIPIENT, ORG, new byte[30], new byte[30], "v1",
                "a".repeat(64), "교육 참여 신청서 수집", RecipientStatus.ACTIVE, 0);
    }

    private static ConsentRequestRecord draft() {
        return new ConsentRequestRecord(REQUEST, "CR-20260825-ABCDEFGH", ORG, RECIPIENT,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TEMPLATE_VERSION,
                UUID.randomUUID(), ConsentRequestStatus.DRAFT, NOW.plusSeconds(3600),
                UUID.randomUUID(), null, null, ACTOR, NOW, 0);
    }
}
