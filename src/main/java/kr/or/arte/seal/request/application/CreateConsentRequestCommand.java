package kr.or.arte.seal.request.application;

import java.time.Instant;
import java.util.UUID;

public record CreateConsentRequestCommand(UUID organizationUnitId, UUID recipientId,
                                          UUID templateVersionId, Instant expiresAt,
                                          UUID idempotencyKey) {
}
