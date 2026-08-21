package kr.or.arte.seal.audit.application;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditEventCommand(
        Instant occurredAt,
        String actorType,
        UUID actorId,
        String actionCode,
        String targetType,
        String targetId,
        String result,
        String reasonCode,
        UUID correlationId,
        Map<String, Object> metadata
) {

    public AuditEventCommand {
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(actorType, "actorType");
        Objects.requireNonNull(actionCode, "actionCode");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(correlationId, "correlationId");
        metadata = Map.copyOf(metadata);
    }
}
