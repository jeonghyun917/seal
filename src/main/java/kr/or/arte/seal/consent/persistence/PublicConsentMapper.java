package kr.or.arte.seal.consent.persistence;

import kr.or.arte.seal.consent.application.ConsentResponseItemRecord;
import kr.or.arte.seal.consent.application.ConsentResponseRecord;
import kr.or.arte.seal.consent.application.ConsentSubmissionResult;
import kr.or.arte.seal.consent.application.PublicConsentItem;
import kr.or.arte.seal.consent.application.PublicConsentSessionRecord;
import kr.or.arte.seal.consent.application.PublicConsentSessionView;
import kr.or.arte.seal.consent.application.PublicLinkRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface PublicConsentMapper {
    Optional<PublicLinkRecord> findLink(@Param("tokenHmac") String tokenHmac);
    Optional<PublicLinkRecord> findLinkForUpdate(@Param("tokenHmac") String tokenHmac);
    void markAccessed(@Param("linkId") UUID linkId, @Param("now") Instant now);
    void insertSession(@Param("session") PublicConsentSessionRecord session);
    Optional<PublicConsentSessionView> findSession(@Param("flowHmac") String flowHmac);
    Optional<PublicConsentSessionView> findSessionForUpdate(@Param("flowHmac") String flowHmac);
    List<PublicConsentItem> findItems(@Param("requestId") UUID requestId);
    Optional<ConsentSubmissionResult> findResponse(@Param("requestId") UUID requestId,
            @Param("idempotencyKey") UUID idempotencyKey);
    void insertResponse(@Param("response") ConsentResponseRecord response);
    void insertResponseItem(@Param("item") ConsentResponseItemRecord item);
    int completeRequest(@Param("requestId") UUID requestId, @Param("now") Instant now);
    void completeLink(@Param("linkId") UUID linkId, @Param("now") Instant now);
    void completeSession(@Param("sessionId") UUID sessionId, @Param("now") Instant now);
    void insertCompletedEvent(@Param("requestId") UUID requestId, @Param("now") Instant now);
    int expireSessions(@Param("requestId") UUID requestId, @Param("now") Instant now);
    int expireSession(@Param("sessionId") UUID sessionId, @Param("now") Instant now);
    int expireDueSessions(@Param("now") Instant now, @Param("limit") int limit);
    int expireOtherSessions(@Param("requestId") UUID requestId,
            @Param("sessionId") UUID sessionId, @Param("now") Instant now);
}
