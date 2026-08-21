package kr.or.arte.seal.audit.application.query;

import java.time.Instant;
import java.util.UUID;

public record AuditEventSummary(
        long eventId,
        Instant occurredAt,
        String actorType,
        String actorDisplayName,
        String actionCode,
        String targetType,
        String targetId,
        String result,
        String reasonCode,
        UUID correlationId
) {
}
