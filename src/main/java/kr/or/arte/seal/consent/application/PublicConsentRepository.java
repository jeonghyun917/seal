package kr.or.arte.seal.consent.application;
import java.time.Instant; import java.util.*;
public interface PublicConsentRepository {
 Optional<PublicLinkRecord> findLink(String tokenHmac); Optional<PublicLinkRecord> findLinkForUpdate(String tokenHmac);
 void markAccessed(UUID linkId,Instant now); void insertSession(PublicConsentSessionRecord session);
 default Optional<PublicLinkRecord> findLinkCandidates(List<String> hashes){for(String hash:hashes){var found=findLink(hash);if(found.isPresent())return found;}return Optional.empty();}
 default Optional<PublicLinkRecord> findLinkForUpdateCandidates(List<String> hashes){for(String hash:hashes){var found=findLinkForUpdate(hash);if(found.isPresent())return found;}return Optional.empty();}
 default Optional<PublicConsentSessionView> findSessionForUpdate(String flowHmac,Instant now){return Optional.empty();}
 default Optional<PublicConsentSessionView> findSession(String flowHmac){return Optional.empty();}
 default Optional<PublicConsentSessionView> findSessionForUpdateCandidates(List<String> hashes,Instant now){for(String hash:hashes){var found=findSessionForUpdate(hash,now);if(found.isPresent())return found;}return Optional.empty();}
 default Optional<PublicConsentSessionView> findSessionCandidates(List<String> hashes){for(String hash:hashes){var found=findSession(hash);if(found.isPresent())return found;}return Optional.empty();}
 default List<PublicConsentItem> findItems(UUID requestId){return List.of();}
 default Optional<ConsentSubmissionResult> findResponse(UUID requestId,UUID idempotencyKey){return Optional.empty();}
 default void insertResponse(ConsentResponseRecord response,List<ConsentResponseItemRecord> items){throw new UnsupportedOperationException();}
 default void complete(UUID requestId,UUID linkId,UUID sessionId,Instant now){throw new UnsupportedOperationException();}
}
