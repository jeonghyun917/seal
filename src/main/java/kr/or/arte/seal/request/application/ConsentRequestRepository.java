package kr.or.arte.seal.request.application;

import kr.or.arte.seal.request.domain.ConsentRequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRequestRepository {
    Optional<ConsentRequestRecord> findByIdempotency(UUID actorId, UUID organizationId, UUID idempotencyKey);
    void insert(ConsentRequestRecord row, UUID actorId, Instant now);
    Optional<ConsentRequestRecord> findScoped(UUID actorId, UUID requestId);
    Optional<ConsentRequestRecord> findScopedForUpdate(UUID actorId, UUID requestId);
    void insertSnapshot(UUID requestId, UUID snapshotId, RequestSnapshot snapshot,
                        UUID actorId, Instant now);
    boolean markReady(UUID requestId, UUID snapshotId, UUID actorId, Instant now, long versionNo);
    boolean cancel(UUID requestId, UUID actorId, Instant now, String reason, long versionNo);
    void insertEvent(UUID requestId, String eventType, String actorType, UUID actorId,
                     String reasonCode, Instant now);
    List<ConsentRequestRecord> searchScoped(UUID actorId, ConsentRequestStatus status);
    default boolean updateDraft(ConsentRequestRecord changed, UUID actorId, Instant now, long versionNo) { return false; }
    default List<ConsentRequestRecord> findDueForUpdate(Instant now, int limit) { return List.of(); }
    default boolean markExpired(UUID requestId, Instant now, long versionNo) { return false; }
    default Optional<String> findSnapshotJsonScoped(UUID actorId, UUID requestId) { return Optional.empty(); }
}
