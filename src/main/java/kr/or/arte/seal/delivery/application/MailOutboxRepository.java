package kr.or.arte.seal.delivery.application;

import java.time.Instant;
import java.util.*;

public interface MailOutboxRepository {
    List<MailOutboxRecord> claimDue(Instant now, int limit, Instant leaseExpiredBefore);
    void accepted(UUID id, int attempt, long versionNo, Instant now);
    void retry(UUID id, int attempt, long versionNo, Instant next, String code, Instant now);
    void failed(UUID id, int attempt, long versionNo, String code, Instant now);

    default void insert(MailOutboxRecord row, UUID actorId, Instant now) { throw new UnsupportedOperationException(); }
    default void cancelPending(UUID requestId, Instant now) { } default boolean isCurrentActiveLink(UUID outboxId,UUID linkId,Instant now){return true;} default void cancelClaim(UUID outboxId,long versionNo,Instant now){throw new UnsupportedOperationException();}
    default List<MailOutboxRecord> search(UUID actorId, MailOutboxStatus status) { return List.of(); } default List<MailOutboxRecord> search(UUID actorId,MailOutboxStatus status,int limit,int offset){return search(actorId,status).stream().skip(offset).limit(limit).toList();}
    default Optional<MailOutboxRecord> findScoped(UUID actorId, UUID outboxId) { return Optional.empty(); }
    default void requeue(UUID actorId, UUID outboxId, Instant now) { throw new UnsupportedOperationException(); }
}
