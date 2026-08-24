package kr.or.arte.seal.consent.persistence;
import kr.or.arte.seal.consent.application.*; import org.apache.ibatis.annotations.*;
import java.time.Instant; import java.util.*;
@Mapper public interface PublicConsentMapper {
 Optional<PublicLinkRecord> findLink(@Param("tokenHmac")String tokenHmac);Optional<PublicLinkRecord> findLinkForUpdate(@Param("tokenHmac")String tokenHmac);
 void markAccessed(@Param("linkId")UUID linkId,@Param("now")Instant now);void insertSession(@Param("session")PublicConsentSessionRecord session);
 Optional<PublicConsentSessionView> findSession(@Param("flowHmac")String flowHmac);Optional<PublicConsentSessionView> findSessionForUpdate(@Param("flowHmac")String flowHmac);
 List<PublicConsentItem> findItems(@Param("requestId")UUID requestId);Optional<ConsentSubmissionResult> findResponse(@Param("requestId")UUID requestId,@Param("idempotencyKey")UUID idempotencyKey);
 void insertResponse(@Param("response")ConsentResponseRecord response);void insertResponseItem(@Param("item")ConsentResponseItemRecord item);
 int completeRequest(@Param("requestId")UUID requestId,@Param("now")Instant now);void completeLink(@Param("linkId")UUID linkId,@Param("now")Instant now);void completeSession(@Param("sessionId")UUID sessionId,@Param("now")Instant now);void insertCompletedEvent(@Param("requestId")UUID requestId,@Param("now")Instant now);
}
