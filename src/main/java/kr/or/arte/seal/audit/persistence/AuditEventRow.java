package kr.or.arte.seal.audit.persistence;

import java.time.Instant;
import java.util.UUID;

public record AuditEventRow(
        Instant occurredAt,
        String actorType,
        UUID actorId,
        String actionCode,
        String targetType,
        String targetId,
        String result,
        String reasonCode,
        UUID correlationId,
        String metadataJson
) {
}
