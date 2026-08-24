package kr.or.arte.seal.request.application;

import java.time.Instant;
import java.util.UUID;

public record UpdateConsentRequestCommand(UUID recipientId, UUID templateVersionId,
                                          Instant expiresAt, long versionNo) { }
