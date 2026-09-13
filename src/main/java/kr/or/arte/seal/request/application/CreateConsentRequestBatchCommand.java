package kr.or.arte.seal.request.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateConsentRequestBatchCommand(List<UUID> recipientIds, UUID templateVersionId,
        Instant expiresAt, UUID idempotencyKey) {
}
