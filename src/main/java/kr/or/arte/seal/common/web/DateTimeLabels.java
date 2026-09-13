package kr.or.arte.seal.common.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeLabels {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Seoul"));

    private DateTimeLabels() {
    }

    public static String format(Instant instant) {
        return instant == null ? "예정 없음" : DATE_TIME.format(instant);
    }
}
