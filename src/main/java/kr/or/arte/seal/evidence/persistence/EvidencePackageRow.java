package kr.or.arte.seal.evidence.persistence;

import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;

import java.time.Instant;
import java.util.UUID;

public record EvidencePackageRow(UUID packageId, UUID requestId, String requestCode, String templateTitle,
        EvidenceGenerationStatus status,
        String relativePath, String manifestSha256, String manifestHmac, String hmacKeyId,
        Instant queuedAt, Instant finalizedAt, String lastErrorCode) {
}
