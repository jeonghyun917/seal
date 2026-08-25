package kr.or.arte.seal.retention.application;

import kr.or.arte.seal.retention.domain.RetentionJobStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionJobRecordTest {
    @Test
    void approvedStartsWithJobIdAndExecutingResumesWithStoredKey() {
        UUID jobId = UUID.randomUUID();
        UUID storedKey = UUID.randomUUID();

        RetentionJobRecord approved = record(jobId, RetentionJobStatus.APPROVED, null);
        RetentionJobRecord executing = record(jobId, RetentionJobStatus.EXECUTING, storedKey);
        RetentionJobRecord failed = record(jobId, RetentionJobStatus.FAILED, storedKey);

        assertThat(approved.executionAvailable()).isTrue();
        assertThat(approved.executionKey()).isEqualTo(jobId);
        assertThat(executing.executionAvailable()).isTrue();
        assertThat(executing.executionKey()).isEqualTo(storedKey);
        assertThat(failed.executionAvailable()).isFalse();
        assertThat(failed.executionKey()).isNull();
    }

    private static RetentionJobRecord record(UUID id, RetentionJobStatus status, UUID key) {
        return new RetentionJobRecord(id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                null, LocalDate.now(), LocalDate.now(), status, "LOGICAL_DELETE", key,
                UUID.randomUUID(), Instant.now(), UUID.randomUUID(), null, null,
                0, 0, 0, 0, false, 0);
    }
}
