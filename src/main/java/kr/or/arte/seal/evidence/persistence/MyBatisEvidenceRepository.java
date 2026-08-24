package kr.or.arte.seal.evidence.persistence;

import kr.or.arte.seal.evidence.application.*;
import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Repository
public class MyBatisEvidenceRepository implements EvidenceRepository {
    private final EvidenceMapper mapper;
    public MyBatisEvidenceRepository(EvidenceMapper mapper){this.mapper=mapper;}
    public void enqueue(UUID responseId,UUID requestId,Instant at){mapper.enqueue(UUID.randomUUID(),responseId,requestId,at);}
    @Transactional public List<EvidenceWorkItem> claimDue(Instant now,int limit,Instant lease){
        return mapper.claimDue(now,limit,lease).stream().map(row->new EvidenceWorkItem(row.packageId(),row.responseId(),
                row.requestId(),row.organizationId(),row.title(),row.snapshotJson(),row.submittedAt(),
                mapper.decisions(row.responseId()),mapper.events(row.requestId()),row.attemptCount())).toList();
    }
    @Transactional public void finalized(UUID id,GeneratedEvidence generated,Instant now){
        generated.files().forEach(file->mapper.insertFile(id,file,now)); mapper.finalizePackage(id,generated,now);
    }
    public void failed(UUID id,String code,Instant now){mapper.fail(id,code,now);}
    public List<EvidencePackageSummary> search(UUID actorId,EvidenceGenerationStatus status){return mapper.search(actorId,status);}
    public Optional<EvidencePackageDetail> findScoped(UUID actorId,UUID id){return mapper.findScoped(actorId,id).map(p->new EvidencePackageDetail(p.packageId(),p.requestId(),p.status(),p.relativePath(),p.manifestSha256(),p.manifestHmac(),p.queuedAt(),p.finalizedAt(),p.lastErrorCode(),mapper.files(id)));}
    public void requeue(UUID actorId,UUID id,Instant now){if(mapper.requeue(actorId,id,now)!=1)throw new IllegalStateException("재생성할 수 없는 증적입니다.");}
    public void recordVerification(UUID packageId,UUID actorId,EvidenceVerificationResult result,Instant now){mapper.verification(UUID.randomUUID(),packageId,actorId,result,now);}
}
