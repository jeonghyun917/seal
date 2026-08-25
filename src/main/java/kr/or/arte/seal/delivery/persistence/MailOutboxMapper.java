package kr.or.arte.seal.delivery.persistence;

import kr.or.arte.seal.delivery.application.*;
import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.*;

@Mapper
public interface MailOutboxMapper {
    void insert(@Param("row") MailOutboxRecord row, @Param("actorId") UUID actorId,
            @Param("now") Instant now);
    void cancelPending(@Param("requestId") UUID requestId, @Param("now") Instant now); boolean isCurrentActiveLink(@Param("outboxId") UUID outboxId,@Param("linkId") UUID linkId,@Param("now") Instant now); int cancelClaim(@Param("outboxId") UUID outboxId,@Param("versionNo") long versionNo,@Param("now") Instant now);
    List<MailOutboxRecord> claimDue(@Param("now") Instant now, @Param("limit") int limit,
            @Param("leaseExpiredBefore") Instant leaseExpiredBefore);
    int accepted(@Param("id") UUID id, @Param("attempt") int attempt, @Param("versionNo") long versionNo,
            @Param("now") Instant now);
    int retry(@Param("id") UUID id, @Param("attempt") int attempt, @Param("versionNo") long versionNo,
            @Param("next") Instant next, @Param("code") String code, @Param("now") Instant now);
    int failed(@Param("id") UUID id, @Param("attempt") int attempt, @Param("versionNo") long versionNo,
            @Param("code") String code, @Param("now") Instant now);
    void insertAttempt(@Param("id") UUID id, @Param("attempt") int attempt,
            @Param("result") String result, @Param("code") String code, @Param("now") Instant now);
    List<MailOutboxRecord> search(@Param("actorId") UUID actorId, @Param("status") MailOutboxStatus status,@Param("limit") int limit,@Param("offset") int offset);
    Optional<MailOutboxRecord> findScoped(@Param("actorId") UUID actorId, @Param("outboxId") UUID outboxId);
    int requeue(@Param("actorId") UUID actorId, @Param("outboxId") UUID outboxId,
            @Param("now") Instant now);
}
