package kr.or.arte.seal.evidence.persistence;

import kr.or.arte.seal.evidence.application.EvidenceClaimLostException;
import kr.or.arte.seal.evidence.application.EvidencePackageDetail;
import kr.or.arte.seal.evidence.application.EvidencePackageSummary;
import kr.or.arte.seal.evidence.application.EvidenceRepository;
import kr.or.arte.seal.evidence.application.EvidenceVerificationResult;
import kr.or.arte.seal.evidence.application.EvidenceWorkItem;
import kr.or.arte.seal.evidence.application.GeneratedEvidence;
import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MyBatisEvidenceRepository implements EvidenceRepository {
    private final EvidenceMapper mapper;

    public MyBatisEvidenceRepository(EvidenceMapper mapper) {
        this.mapper = mapper;
    }

    public void enqueue(UUID responseId, UUID requestId, Instant at) {
        mapper.enqueue(UUID.randomUUID(), responseId, requestId, at);
    }

    @Transactional
    public List<EvidenceWorkItem> claimDue(Instant now, int limit, Instant lease) {
        return mapper.claimDue(now, limit, lease).stream()
                .map(row -> new EvidenceWorkItem(row.packageId(), row.responseId(), row.requestId(),
                        row.organizationId(), row.title(), row.snapshotJson(), row.submittedAt(),
                        mapper.decisions(row.responseId()), mapper.events(row.requestId()),
                        row.attemptCount(), row.rowVersion()))
                .toList();
    }

    @Transactional
    public void finalized(UUID id, long claimedRowVersion, GeneratedEvidence generated,
            Instant now) {
        if (mapper.finalizePackage(id, claimedRowVersion, generated, now) != 1) {
            throw new EvidenceClaimLostException("처리 권한을 잃어 증적 패키지를 최종화할 수 없습니다.");
        }
        generated.files().forEach(file -> mapper.insertFile(id, file, now));
    }

    public void failed(UUID id, long claimedRowVersion, String code, Instant now) {
        if (mapper.fail(id, claimedRowVersion, code, now) != 1) {
            throw new EvidenceClaimLostException("처리 권한을 잃어 증적 실패 상태를 기록할 수 없습니다.");
        }
    }

    public List<EvidencePackageSummary> search(UUID actorId, EvidenceGenerationStatus status) {
        return mapper.search(actorId, status, 100, 0);
    }

    public Optional<EvidencePackageDetail> findScoped(UUID actorId, UUID id) {
        return mapper.findScoped(actorId, id)
                .map(row -> new EvidencePackageDetail(row.packageId(), row.requestId(), row.status(),
                        row.relativePath(), row.manifestSha256(), row.manifestHmac(), row.hmacKeyId(),
                        row.queuedAt(), row.finalizedAt(), row.lastErrorCode(), mapper.files(id)));
    }

    public void requeue(UUID actorId, UUID id, Instant now) {
        if (mapper.requeue(actorId, id, now) != 1) {
            throw new IllegalStateException("재생성할 수 없는 증적입니다.");
        }
    }

    public void recordVerification(UUID packageId, UUID actorId,
            EvidenceVerificationResult result, Instant now) {
        mapper.verification(UUID.randomUUID(), packageId, actorId, result, now);
    }

    public List<EvidencePackageSummary> search(UUID actor, EvidenceGenerationStatus status,
            int limit, int offset) {
        return mapper.search(actor, status, limit, offset);
    }
}
