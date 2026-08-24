package kr.or.arte.seal.link.persistence;

import kr.or.arte.seal.link.application.SecureLinkInsert;
import kr.or.arte.seal.link.application.SecureLinkRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class MyBatisSecureLinkRepository implements SecureLinkRepository {
    private final SecureLinkMapper mapper;
    public MyBatisSecureLinkRepository(SecureLinkMapper mapper) { this.mapper = mapper; }
    @Override public void insert(SecureLinkInsert value) { requireOne(mapper.insert(value)); }
    @Override public void revokeCurrent(UUID requestId, UUID actorId, Instant now, String reason) {
        mapper.revokeCurrent(requestId, actorId, now, reason);
    }
    @Override public boolean activateRequest(UUID requestId, UUID linkId, UUID actorId,
            Instant now, long versionNo) {
        return mapper.activateRequest(requestId, linkId, actorId, now, versionNo) == 1;
    }
    @Override public boolean replaceCurrentLink(UUID requestId, UUID linkId, UUID actorId,
            Instant now, long versionNo) {
        return mapper.replaceCurrentLink(requestId, linkId, actorId, now, versionNo) == 1;
    }
    private static void requireOne(int count) {
        if (count != 1) throw new IllegalStateException("보안 링크 저장 건수가 일치하지 않습니다.");
    }
}
