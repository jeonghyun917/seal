package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import java.time.Instant;
import java.util.*;

public interface EvidenceRepository extends EvidenceQueue {
    List<EvidenceWorkItem> claimDue(Instant now, int limit, Instant leaseExpiredAt);
    void generateAndFinalize(EvidenceWorkItem item, EvidenceArtifactGenerator generator, Instant now);
    void finalized(UUID packageId, long claimedRowVersion, GeneratedEvidence generated, Instant now);
    void failed(UUID packageId, long claimedRowVersion, String errorCode, Instant now);
    List<EvidencePackageSummary> search(UUID actorId, EvidenceGenerationStatus status); default List<EvidencePackageSummary> search(UUID actorId,EvidenceGenerationStatus status,int limit,int offset){return search(actorId,status).stream().skip(offset).limit(limit).toList();}
    default List<EvidencePackageSummary> search(UUID actorId, EvidenceGenerationStatus status,
            EvidenceSearchFilter filter, int limit, int offset) {
        return search(actorId, status).stream()
                .filter(item -> filter.query() == null || item.templateTitle().toLowerCase(Locale.ROOT).contains(filter.query().toLowerCase(Locale.ROOT))
                        || item.requestCode().toLowerCase(Locale.ROOT).contains(filter.query().toLowerCase(Locale.ROOT)))
                .filter(item -> filter.fromInclusive() == null || !item.queuedAt().isBefore(filter.fromInclusive()))
                .filter(item -> filter.untilExclusive() == null || item.queuedAt().isBefore(filter.untilExclusive()))
                .skip(offset).limit(limit).toList();
    }
    Optional<EvidencePackageDetail> findScoped(UUID actorId, UUID packageId);
    default Optional<EvidencePackageDetail> findByRequestScoped(UUID actorId, UUID requestId) {
        return Optional.empty();
    }
    void requeue(UUID actorId, UUID packageId, Instant now);
    void recordVerification(UUID packageId, UUID actorId, EvidenceVerificationResult result, Instant now);
}
