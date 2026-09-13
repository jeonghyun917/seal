package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvidencePackageDetail(UUID packageId, UUID requestId, String requestCode, String templateTitle,
        EvidenceGenerationStatus status,
        String relativePath, String manifestSha256, String manifestHmac, String hmacKeyId,
        Instant queuedAt, Instant finalizedAt, String lastErrorCode, List<EvidenceFileMetadata> files) {
    public EvidencePackageDetail { files = files == null ? List.of() : List.copyOf(files); }
}
