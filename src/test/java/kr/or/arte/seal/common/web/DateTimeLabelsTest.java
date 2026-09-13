package kr.or.arte.seal.common.web;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeLabelsTest {

    @Test
    void formatsUtcInstantAsReadableKoreanLocalTime() {
        assertThat(DateTimeLabels.format(Instant.parse("2026-08-24T09:20:58.417353Z")))
                .isEqualTo("2026-08-24 18:20:58");
    }

    @Test
    void describesMissingSchedule() {
        assertThat(DateTimeLabels.format(null)).isEqualTo("예정 없음");
    }
}
