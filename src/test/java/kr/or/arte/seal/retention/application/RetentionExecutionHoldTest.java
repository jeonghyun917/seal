package kr.or.arte.seal.retention.application;

import kr.or.arte.seal.retention.domain.RetentionJobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetentionExecutionHoldTest {
    @TempDir Path root;
    private final UUID actor = UUID.randomUUID(), job = UUID.randomUUID(), request = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-29T00:00:00Z");

    @Test void holdAddedAfterApprovalPreventsExecutionBeforeAnyFilesOrDataAreChanged() {
        var repository = mock(RetentionRepository.class);
        when(repository.findScopedForUpdate(actor, job)).thenReturn(Optional.of(approved()));
        when(repository.hasActiveHold(request, LocalDate.of(2026, 9, 29))).thenReturn(true);
        var service = new RetentionJobService(repository, root.toString(), Clock.fixed(now, ZoneOffset.UTC), c -> {});

        assertThatThrownBy(() -> service.execute(actor, job, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("보류");
        verify(repository, never()).evidencePaths(any());
        verify(repository, never()).destroyPrimaryData(any(), any(), any(), anyInt(), any());
        verify(repository, never()).transition(any(), any(), any(), any(), any(), any(), any(), anyLong());
    }

    private RetentionJobRecord approved() {
        return new RetentionJobRecord(job, request, UUID.randomUUID(), UUID.randomUUID(), null,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), RetentionJobStatus.APPROVED,
                "DELETE", null, actor, now, UUID.randomUUID(), null, null, 0, 0, 0, 0, false, 2);
    }

    @Test void cannotPlaceLegalHoldAfterDestructionStarts() {
        var repository = mock(RetentionRepository.class);
        when(repository.hasStartedDestruction(request)).thenReturn(true);
        var service = new RetentionJobService(repository, root.toString(), Clock.fixed(now, ZoneOffset.UTC), c -> {});
        assertThatThrownBy(() -> service.placeLegalHold(actor, request, "AUDIT", "감사 자료 보존", null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("파기가 시작");
        verify(repository, never()).placeLegalHold(any(), any(), any(), any(), any(), any());
    }

    @Test void cannotPlaceRestrictionHoldAfterDestructionStarts() {
        var repository = mock(RetentionRepository.class);
        when(repository.hasStartedDestruction(request)).thenReturn(true);
        var service = new RetentionJobService(repository, root.toString(), Clock.fixed(now, ZoneOffset.UTC), c -> {});
        assertThatThrownBy(() -> service.placeRestrictionHold(request, actor, now))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("파기가 시작");
        verify(repository, never()).placeRestrictionHold(any(), any(), any(), any());
    }
}
