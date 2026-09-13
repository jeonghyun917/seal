package kr.or.arte.seal.statistics.persistence;

import kr.or.arte.seal.statistics.application.StatisticsRepository;
import kr.or.arte.seal.statistics.application.StatisticsSnapshot;
import kr.or.arte.seal.statistics.application.StatusCount;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class MyBatisStatisticsRepository implements StatisticsRepository {
    private final StatisticsMapper mapper;

    public MyBatisStatisticsRepository(StatisticsMapper mapper) { this.mapper = mapper; }

    public StatisticsSnapshot query(UUID actor, LocalDate from, LocalDate to) {
        var rows = mapper.query(actor, from, to);
        return new StatisticsSnapshot(group(rows, "REQUEST"), group(rows, "RESPONSE"),
                group(rows, "RESPONSE_WARNING"), group(rows, "MAIL"), group(rows, "EVIDENCE"),
                group(rows, "EVIDENCE_WARNING"), group(rows, "RIGHTS"), group(rows, "RIGHTS_WARNING"),
                group(rows, "RETENTION"), group(rows, "RETENTION_WARNING"));
    }

    private static List<StatusCount> group(List<MetricCount> rows, String category) {
        return rows.stream().filter(row -> category.equals(row.category()))
                .map(row -> new StatusCount(row.status(), row.count())).toList();
    }
}
