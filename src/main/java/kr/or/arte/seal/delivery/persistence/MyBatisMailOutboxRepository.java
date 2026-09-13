package kr.or.arte.seal.delivery.persistence;

import kr.or.arte.seal.delivery.application.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Repository
public class MyBatisMailOutboxRepository implements MailOutboxRepository {
    private final MailOutboxMapper mapper;

    public MyBatisMailOutboxRepository(MailOutboxMapper mapper) { this.mapper = mapper; }
    public void insert(MailOutboxRecord row, UUID actorId, Instant now) { mapper.insert(row, actorId, now); }
    public void cancelPending(UUID requestId, Instant now) { mapper.cancelPending(requestId, now); } public boolean isCurrentActiveLink(UUID outboxId,UUID linkId,Instant now){return mapper.isCurrentActiveLink(outboxId,linkId,now);} public void cancelClaim(UUID outboxId,long versionNo,Instant now){requireUpdated(mapper.cancelClaim(outboxId,versionNo,now));}
    public List<MailOutboxRecord> claimDue(Instant now, int limit, Instant lease) {
        return mapper.claimDue(now, limit, lease);
    }

    @Transactional
    public void accepted(UUID id, int attempt, long versionNo, Instant now) {
        requireUpdated(mapper.accepted(id, attempt, versionNo, now));
        mapper.insertAttempt(id, attempt, "SMTP_ACCEPTED", null, now);
    }

    @Transactional
    public void retry(UUID id, int attempt, long versionNo, Instant next, String code, Instant now) {
        requireUpdated(mapper.retry(id, attempt, versionNo, next, code, now));
        mapper.insertAttempt(id, attempt, "RETRY_SCHEDULED", code, now);
    }

    @Transactional
    public void failed(UUID id, int attempt, long versionNo, String code, Instant now) {
        requireUpdated(mapper.failed(id, attempt, versionNo, code, now));
        mapper.insertAttempt(id, attempt, "SEND_FAILED", code, now);
    }

    public List<MailOutboxRecord> search(UUID actorId, MailOutboxStatus status) {
        return mapper.search(actorId, status, null, 100, 0);
    }
    public Optional<MailOutboxRecord> findScoped(UUID actorId, UUID id) { return mapper.findScoped(actorId, id); }
    public void requeue(UUID actorId, UUID id, Instant now) {
        if (mapper.requeue(actorId, id, now) != 1) throw new IllegalStateException("재시도할 수 없는 메일입니다.");
    }

    private static void requireUpdated(int affected) {
        if (affected != 1) throw new IllegalStateException("메일 처리 상태가 이미 변경되었습니다.");
    }
    public List<MailOutboxRecord> search(UUID actor,MailOutboxStatus status,int limit,int offset){return mapper.search(actor,status,null,limit,offset);}
    @Override public List<MailOutboxRecord> search(UUID actor, MailOutboxStatus status, String query,
            int limit, int offset) { return mapper.search(actor, status, query, limit, offset); }
}
