package kr.or.arte.seal.delivery.web;

import kr.or.arte.seal.delivery.application.MailOutboxDetail;
import kr.or.arte.seal.delivery.application.MailOutboxStatus;

import java.time.Instant;
import java.util.Optional;

public record MailSchedulePresentation(String label, Instant at) {

    public static Optional<MailSchedulePresentation> from(MailOutboxDetail mailItem) {
        if (mailItem.status() == MailOutboxStatus.QUEUED) {
            return Optional.ofNullable(mailItem.scheduledAt())
                    .map(at -> new MailSchedulePresentation("발송 처리 예정", at));
        }
        if (mailItem.status() == MailOutboxStatus.RETRY_SCHEDULED) {
            return Optional.ofNullable(mailItem.nextAttemptAt())
                    .map(at -> new MailSchedulePresentation("재시도 예정", at));
        }
        if (mailItem.status() == MailOutboxStatus.SMTP_ACCEPTED) {
            return Optional.ofNullable(mailItem.smtpAcceptedAt())
                    .map(at -> new MailSchedulePresentation("발송 완료 시간", at));
        }
        return Optional.empty();
    }
}
