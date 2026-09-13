package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import kr.or.arte.seal.recipient.application.RecipientDetail;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvidenceQueryServiceTest {
    @Test
    void listShowsHumanReadableRequestRecipientAndResponseSummary(@TempDir Path evidenceRoot) {
        EvidenceRepository repository = mock(EvidenceRepository.class);
        RecipientQueryService recipients = mock(RecipientQueryService.class);
        UUID actorId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        Instant queuedAt = Instant.parse("2026-08-28T05:47:33Z");
        Instant finalizedAt = Instant.parse("2026-08-28T05:48:06Z");
        EvidencePackageSummary summary = new EvidencePackageSummary(packageId, requestId,
                "CR-20260828-ABCDEFGH", "개인정보 수집·이용 동의 안내", recipientId,
                2, 3, EvidenceGenerationStatus.FINALIZED, queuedAt, finalizedAt, 1, null);
        when(repository.search(actorId, null, 21, 0)).thenReturn(List.of(summary));
        when(recipients.find(actorId, recipientId)).thenReturn(new RecipientDetail(recipientId,
                organizationId, "한국문화예술교육진흥원", "과장", "고*현", "v********@a******.kr",
                "교육 신청서", RecipientStatus.ACTIVE, 0));
        EvidenceQueryService service = new EvidenceQueryService(repository, evidenceRoot.toString(),
                "", "v1", Clock.fixed(queuedAt, ZoneOffset.UTC), mock(AuditEventWriter.class), recipients);

        EvidencePackageListItem item = service.searchPage(actorId, null, 1, 20).items().get(0);

        assertThat(item.requestCode()).isEqualTo("CR-20260828-ABCDEFGH");
        assertThat(item.templateTitle()).isEqualTo("개인정보 수집·이용 동의 안내");
        assertThat(item.maskedName()).isEqualTo("고*현");
        assertThat(item.maskedEmail()).isEqualTo("v********@a******.kr");
        assertThat(item.responseSummary()).isEqualTo("일부 미동의");
    }
}
