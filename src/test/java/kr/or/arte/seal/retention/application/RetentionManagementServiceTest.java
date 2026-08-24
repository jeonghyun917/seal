package kr.or.arte.seal.retention.application;

import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.retention.domain.RetentionAnchorType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RetentionManagementServiceTest {
    private final RetentionRepository repository = mock(RetentionRepository.class);
    private final AuditEventWriter audit = mock(AuditEventWriter.class);
    private final Instant now = Instant.parse("2026-08-24T06:00:00Z");
    private final RetentionJobService service = new RetentionJobService(
            repository,
            "target/test-evidence",
            Clock.fixed(now, ZoneId.of("Asia/Seoul")),
            audit);

    @Test
    void recordsVersionedManualAnchor() {
        UUID actor = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        LocalDate anchorDate = LocalDate.of(2026, 8, 20);

        service.recordAnchor(actor, request, RetentionAnchorType.PROGRAM_END_AT, anchorDate);

        verify(repository).recordAnchor(request, RetentionAnchorType.PROGRAM_END_AT, anchorDate, actor, now);
    }

    @Test
    void validatesAndManagesLegalHold() {
        UUID actor = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        UUID hold = UUID.randomUUID();
        LocalDate expiresOn = LocalDate.of(2026, 9, 30);

        service.placeLegalHold(actor, request, "AUDIT", "감사 대응을 위한 보존", expiresOn);
        service.releaseLegalHold(actor, hold);

        verify(repository).placeLegalHold(request, "AUDIT", "감사 대응을 위한 보존", expiresOn, actor, now);
        verify(repository).releaseLegalHold(hold, actor, now);
        assertThatThrownBy(() -> service.placeLegalHold(actor, request, " ", "근거", expiresOn))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.placeLegalHold(actor, request, "AUDIT", " ", expiresOn))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
