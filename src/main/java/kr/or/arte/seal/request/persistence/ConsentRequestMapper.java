package kr.or.arte.seal.request.persistence;

import kr.or.arte.seal.request.application.ConsentRequestRecord;
import kr.or.arte.seal.request.application.RequestSnapshot;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ConsentRequestMapper {
    List<ConsentRequestRecord> findByIdempotency(@Param("actorId") UUID actorId,
            @Param("organizationId") UUID organizationId, @Param("idempotencyKey") UUID key);
    int insert(@Param("row") ConsentRequestRecord row, @Param("actorId") UUID actorId,
               @Param("now") Instant now);
    int updateDraft(@Param("row") ConsentRequestRecord row, @Param("actorId") UUID actorId,
                    @Param("now") Instant now, @Param("versionNo") long versionNo);
    List<ConsentRequestRecord> findScopedForUpdate(@Param("actorId") UUID actorId,
                                                    @Param("requestId") UUID requestId);
    int insertSnapshot(@Param("requestId") UUID requestId, @Param("snapshotId") UUID snapshotId,
            @Param("snapshot") RequestSnapshot snapshot, @Param("actorId") UUID actorId,
            @Param("now") Instant now);
    int insertItem(@Param("itemId") UUID itemId, @Param("snapshotId") UUID snapshotId,
                   @Param("item") RequestSnapshot.Item item);
    int insertItemData(@Param("itemId") UUID itemId, @Param("snapshotId") UUID snapshotId,
                       @Param("data") RequestSnapshot.DataItem data);
    int markReady(@Param("requestId") UUID requestId, @Param("snapshotId") UUID snapshotId,
                  @Param("actorId") UUID actorId, @Param("now") Instant now,
                  @Param("versionNo") long versionNo);
    int cancel(@Param("requestId") UUID requestId, @Param("actorId") UUID actorId,
               @Param("now") Instant now, @Param("reason") String reason,
               @Param("versionNo") long versionNo);
    int insertEvent(@Param("eventId") UUID eventId, @Param("requestId") UUID requestId,
                    @Param("eventType") String eventType, @Param("actorType") String actorType,
                    @Param("actorId") UUID actorId, @Param("reasonCode") String reasonCode,
                    @Param("now") Instant now);
    List<ConsentRequestRecord> searchScoped(@Param("actorId") UUID actorId,
                                            @Param("status") ConsentRequestStatus status);
    List<ConsentRequestRecord> findDueForUpdate(@Param("now") Instant now, @Param("limit") int limit);
    int markExpired(@Param("requestId") UUID requestId, @Param("now") Instant now,
                    @Param("versionNo") long versionNo);
    List<String> findSnapshotJsonScoped(@Param("actorId") UUID actorId,
                                        @Param("requestId") UUID requestId);
}
