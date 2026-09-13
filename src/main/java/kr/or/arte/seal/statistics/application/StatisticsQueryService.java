package kr.or.arte.seal.statistics.application;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class StatisticsQueryService {
    private static final LocalDate SYSTEM_HISTORY_START = LocalDate.of(2000, 1, 1);
    private final StatisticsRepository repository;

    public StatisticsQueryService(StatisticsRepository repository) {
        this.repository = repository;
    }

    public StatisticsSnapshot query(UUID actor, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusYears(1))) {
            throw new IllegalArgumentException("통계 기간은 순방향 최대 1년입니다.");
        }
        return repository.query(actor, from, to);
    }

    public StatisticsSnapshot queryCurrentWork(UUID actor, LocalDate today) {
        return repository.query(actor, SYSTEM_HISTORY_START, today);
    }
}
