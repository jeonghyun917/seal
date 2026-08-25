package kr.or.arte.seal.retention.application;

import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.retention.domain.RetentionJobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetentionConfirmationPurgeTest {
    @TempDir Path temp;

    @Test
    void confirmationPurgesAnyRemainingJobQuarantineBeforeStateTransition() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID executor = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Path quarantinedFile = temp.resolve(".quarantine").resolve(jobId.toString()).resolve("package/file.json");
        Files.createDirectories(quarantinedFile.getParent());
        Files.writeString(quarantinedFile, "{}");
        RetentionRepository repository = mock(RetentionRepository.class);
        RetentionJobRecord record = new RetentionJobRecord(jobId, requestId, UUID.randomUUID(),
                UUID.randomUUID(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 20),
                RetentionJobStatus.EXECUTED, "LOGICAL_DELETE", UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-08-20T00:00:00Z"), UUID.randomUUID(), executor, null,
                1, 1, 1, 0, true, 5);
        when(repository.findScopedForUpdate(actor, jobId)).thenReturn(Optional.of(record));
        AuditEventWriter audit = command -> { };
        RetentionJobService service = new RetentionJobService(repository, temp.toString(),
                Clock.fixed(Instant.parse("2026-08-25T00:00:00Z"), ZoneOffset.UTC), audit);

        service.confirm(actor, jobId);

        assertThat(Files.exists(temp.resolve(".quarantine").resolve(jobId.toString()))).isFalse();
    }
}
