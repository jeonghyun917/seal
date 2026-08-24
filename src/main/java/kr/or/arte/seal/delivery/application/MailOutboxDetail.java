package kr.or.arte.seal.delivery.application;

import java.time.Instant;
import java.util.UUID;

public record MailOutboxDetail(
        UUID outboxId,
        UUID requestId,
        MailOutboxStatus status,
        String subject,
        Instant scheduledAt,
        Instant nextAttemptAt,
        int attemptCount,
        String html) {
}
