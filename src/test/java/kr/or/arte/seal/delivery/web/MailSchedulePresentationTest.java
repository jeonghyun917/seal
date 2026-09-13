package kr.or.arte.seal.delivery.web;

import kr.or.arte.seal.delivery.application.MailOutboxDetail;
import kr.or.arte.seal.delivery.application.MailOutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MailSchedulePresentationTest {
    private static final Instant SCHEDULED = Instant.parse("2026-08-28T05:46:00Z");
    private static final Instant RETRY = Instant.parse("2026-08-28T05:47:00Z");
    private static final Instant ACCEPTED = Instant.parse("2026-08-28T05:48:00Z");

    @Test
    void queuedMailShowsProcessingSchedule() {
        var view = MailSchedulePresentation.from(detail(MailOutboxStatus.QUEUED));

        assertThat(view).isPresent();
        assertThat(view.orElseThrow().label()).isEqualTo("발송 처리 예정");
        assertThat(view.orElseThrow().at()).isEqualTo(SCHEDULED);
    }

    @Test
    void retryScheduledMailShowsNextRetryTime() {
        var view = MailSchedulePresentation.from(detail(MailOutboxStatus.RETRY_SCHEDULED));

        assertThat(view).isPresent();
        assertThat(view.orElseThrow().label()).isEqualTo("재시도 예정");
        assertThat(view.orElseThrow().at()).isEqualTo(RETRY);
    }

    @Test
    void acceptedMailShowsCompletionTimeInsteadOfFutureSchedule() {
        var view = MailSchedulePresentation.from(detail(MailOutboxStatus.SMTP_ACCEPTED));

        assertThat(view).isPresent();
        assertThat(view.orElseThrow().label()).isEqualTo("발송 완료 시간");
        assertThat(view.orElseThrow().at()).isEqualTo(ACCEPTED);
    }

    private static MailOutboxDetail detail(MailOutboxStatus status) {
        return new MailOutboxDetail(UUID.randomUUID(), UUID.randomUUID(), status, "개인정보 동의",
                SCHEDULED, RETRY, ACCEPTED, 1, "<p>메일</p>");
    }
}
