package kr.or.arte.seal.evidence.persistence;

import kr.or.arte.seal.evidence.application.*;
import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import org.apache.ibatis.annotations.*;
import java.time.Instant;
import java.util.*;

@Mapper
public interface EvidenceMapper {
    void enqueue(@Param("packageId") UUID packageId,@Param("responseId") UUID responseId,
                 @Param("requestId") UUID requestId,@Param("now") Instant now);
    List<EvidenceWorkRow> claimDue(@Param("now") Instant now,@Param("limit") int limit,
                                   @Param("leaseExpiredAt") Instant leaseExpiredAt);
    List<String> decisions(@Param("responseId") UUID responseId);
    List<String> events(@Param("requestId") UUID requestId);
    int finalizePackage(@Param("packageId") UUID packageId,@Param("claimedRowVersion") long claimedRowVersion,@Param("generated") GeneratedEvidence generated,
                        @Param("now") Instant now);
    int insertFile(@Param("packageId") UUID packageId,@Param("file") EvidenceFileMetadata file,
                   @Param("now") Instant now);
    int fail(@Param("packageId") UUID packageId,@Param("claimedRowVersion") long claimedRowVersion,@Param("code") String code,@Param("now") Instant now);
    List<EvidencePackageSummary> search(@Param("actorId") UUID actorId,@Param("status") EvidenceGenerationStatus status,@Param("limit") int limit,@Param("offset") int offset);
    Optional<EvidencePackageRow> findScoped(@Param("actorId") UUID actorId,@Param("packageId") UUID packageId);
    Optional<EvidencePackageRow> findByRequestScoped(@Param("actorId") UUID actorId,
            @Param("requestId") UUID requestId);
    List<EvidenceFileMetadata> files(@Param("packageId") UUID packageId);
    int requeue(@Param("actorId") UUID actorId,@Param("packageId") UUID packageId,@Param("now") Instant now);
    void verification(@Param("id") UUID id,@Param("packageId") UUID packageId,@Param("actorId") UUID actorId,
                      @Param("result") EvidenceVerificationResult result,@Param("now") Instant now);
}
