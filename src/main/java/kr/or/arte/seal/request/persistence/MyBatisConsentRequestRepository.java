package kr.or.arte.seal.request.persistence;

import kr.or.arte.seal.request.application.ConsentRequestRecord;
import kr.or.arte.seal.request.application.ConsentRequestRepository;
import kr.or.arte.seal.request.application.RequestSnapshot;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MyBatisConsentRequestRepository implements ConsentRequestRepository {
    private final ConsentRequestMapper mapper;
    public MyBatisConsentRequestRepository(ConsentRequestMapper mapper) { this.mapper = mapper; }

    @Override public Optional<ConsentRequestRecord> findByIdempotency(UUID actorId, UUID org, UUID key) {
        return mapper.findByIdempotency(actorId, org, key).stream().findFirst();
    }
    @Override public void insert(ConsentRequestRecord row, UUID actorId, Instant now) {
        requireOne(mapper.insert(row, actorId, now));
    }
    @Override public boolean updateDraft(ConsentRequestRecord row, UUID actorId, Instant now, long versionNo) {
        return mapper.updateDraft(row, actorId, now, versionNo) == 1;
    }
    @Override public Optional<ConsentRequestRecord> findScoped(UUID actorId, UUID id) {
        return mapper.findScoped(actorId, id).stream().findFirst();
    }
    @Override public Optional<ConsentRequestRecord> findScopedForUpdate(UUID actorId, UUID id) {
        return mapper.findScopedForUpdate(actorId, id).stream().findFirst();
    }
    @Override public void insertSnapshot(UUID requestId, UUID snapshotId, RequestSnapshot snapshot,
            UUID actorId, Instant now) {
        requireOne(mapper.insertSnapshot(requestId, snapshotId, snapshot, actorId, now));
        for (RequestSnapshot.Item item : snapshot.items()) {
            UUID itemId = UUID.randomUUID();
            requireOne(mapper.insertItem(itemId, snapshotId, item));
            for (RequestSnapshot.DataItem data : item.dataItems()) {
                requireOne(mapper.insertItemData(itemId, snapshotId, data));
            }
        }
    }
    @Override public boolean markReady(UUID id, UUID snapshotId, UUID actor, Instant now, long version) {
        return mapper.markReady(id, snapshotId, actor, now, version) == 1;
    }
    @Override public boolean cancel(UUID id, UUID actor, Instant now, String reason, long version) {
        return mapper.cancel(id, actor, now, reason, version) == 1;
    }
    @Override public void insertEvent(UUID id, String type, String actorType, UUID actor,
            String reason, Instant now) {
        requireOne(mapper.insertEvent(UUID.randomUUID(), id, type, actorType, actor, reason, now));
    }
    @Override public List<ConsentRequestRecord> searchScoped(UUID actor, ConsentRequestStatus status) {
        return mapper.searchScoped(actor, status, null, 100, 0);
    }
    @Override public List<ConsentRequestRecord> searchScopedByRecipient(UUID actor, UUID recipientId) {
        return mapper.searchScopedByRecipient(actor, recipientId);
    }
    @Override public List<ConsentRequestRecord> findDueForUpdate(Instant now, int limit) {
        return mapper.findDueForUpdate(now, limit);
    }
    @Override public boolean markExpired(UUID requestId, Instant now, long versionNo) {
        return mapper.markExpired(requestId, now, versionNo) == 1;
    }
    @Override public Optional<String> findSnapshotJsonScoped(UUID actorId, UUID requestId) {
        return mapper.findSnapshotJsonScoped(actorId, requestId).stream().findFirst();
    }
    private static void requireOne(int count) {
        if (count != 1) throw new IllegalStateException("동의 요청 저장 건수가 일치하지 않습니다.");
    }
    public List<ConsentRequestRecord> searchScoped(UUID actor,ConsentRequestStatus status,int limit,int offset){return mapper.searchScoped(actor,status,null,limit,offset);}
    @Override public List<ConsentRequestRecord> searchScoped(UUID actor, ConsentRequestStatus status, String query,
            int limit, int offset) { return mapper.searchScoped(actor, status, query, limit, offset); }
}
