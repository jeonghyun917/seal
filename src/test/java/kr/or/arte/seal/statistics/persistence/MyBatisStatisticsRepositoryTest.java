package kr.or.arte.seal.statistics.persistence;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MyBatisStatisticsRepositoryTest {

    @Test
    void keepsOverlappingWarningsOutOfExclusiveStatusTotals() {
        StatisticsMapper mapper = mock(StatisticsMapper.class);
        UUID actorId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        given(mapper.query(actorId, from, to)).willReturn(List.of(
                new MetricCount("RESPONSE", "SUBMITTED", 4),
                new MetricCount("RESPONSE_WARNING", "WITHDRAWN", 2),
                new MetricCount("EVIDENCE", "FINALIZED", 3),
                new MetricCount("EVIDENCE_WARNING", "VERIFY_INVALID_HASH", 1),
                new MetricCount("RIGHTS", "IN_REVIEW", 1),
                new MetricCount("RIGHTS_WARNING", "OVERDUE", 1),
                new MetricCount("RETENTION", "DUE", 2),
                new MetricCount("RETENTION_WARNING", "LEGAL_HOLD_ACTIVE", 1)));

        var snapshot = new MyBatisStatisticsRepository(mapper).query(actorId, from, to);

        assertThat(snapshot.responses()).extracting("status", "count")
                .containsExactly(tuple("SUBMITTED", 4L));
        assertThat(snapshot.responseWarnings()).extracting("status", "count")
                .containsExactly(tuple("WITHDRAWN", 2L));
        assertThat(snapshot.evidence()).extracting("status", "count")
                .containsExactly(tuple("FINALIZED", 3L));
        assertThat(snapshot.evidenceWarnings()).extracting("status", "count")
                .containsExactly(tuple("VERIFY_INVALID_HASH", 1L));
        assertThat(snapshot.rights()).extracting("status", "count")
                .containsExactly(tuple("IN_REVIEW", 1L));
        assertThat(snapshot.rightsWarnings()).extracting("status", "count")
                .containsExactly(tuple("OVERDUE", 1L));
        assertThat(snapshot.retention()).extracting("status", "count")
                .containsExactly(tuple("DUE", 2L));
        assertThat(snapshot.retentionWarnings()).extracting("status", "count")
                .containsExactly(tuple("LEGAL_HOLD_ACTIVE", 1L));
    }
}
