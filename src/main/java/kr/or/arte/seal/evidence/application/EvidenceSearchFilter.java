package kr.or.arte.seal.evidence.application;

import java.time.*;

public record EvidenceSearchFilter(String query, Instant fromInclusive, Instant untilExclusive) {
    public static EvidenceSearchFilter of(String query, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        String normalized = query == null || query.isBlank() ? null : query.trim();
        if (normalized != null && normalized.length() > 200) {
            throw new IllegalArgumentException("검색어는 200자 이내로 입력해 주세요.");
        }
        ZoneId zone = ZoneId.of("Asia/Seoul");
        return new EvidenceSearchFilter(normalized,
                from == null ? null : from.atStartOfDay(zone).toInstant(),
                to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant());
    }
}
