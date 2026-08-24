package kr.or.arte.seal.link.application;

import kr.or.arte.seal.link.domain.SecureLinkStatus;

import java.time.Instant;
import java.util.UUID;

public record SecureLinkInsert(UUID linkId, UUID requestId, String tokenHmac,
                               SecureLinkStatus status, Instant expiresAt,
                               UUID issuedBy, Instant issuedAt) {
    @Override public String toString() {
        return "SecureLinkInsert[linkId=" + linkId + ", requestId=" + requestId
                + ", status=" + status + ", expiresAt=" + expiresAt + "]";
    }
}
