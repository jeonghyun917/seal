package kr.or.arte.seal.request.application;

import kr.or.arte.seal.request.domain.ConsentRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record ConsentRequestSummary(UUID requestId, String requestCode, UUID recipientId,
                                    ConsentRequestStatus status, Instant expiresAt,
                                    Instant createdAt, long versionNo) { }
