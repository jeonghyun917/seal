package kr.or.arte.seal.rights.application;
import kr.or.arte.seal.rights.domain.*;import java.time.*;import java.util.*;
public interface RightsRequestRepository {
 void insert(RightsRequestRecord row);List<RightsRequestSummary> search(UUID actor,RightsRequestStatus status);Optional<RightsRequestRecord> findScoped(UUID actor,UUID id);Optional<RightsRequestRecord> findScopedForUpdate(UUID actor,UUID id);
 void transition(UUID id,RightsRequestStatus from,RightsRequestStatus to,UUID actor,String channel,byte[] verificationNote,String keyId,String decisionCode,byte[] resultNote,Instant now,long version);
 void insertEvent(UUID id,RightsRequestStatus from,RightsRequestStatus to,UUID actor,String reason,Instant now);
 void insertWithdrawal(UUID rightsId,UUID responseId,UUID actor,Instant now);Optional<UUID> responseId(UUID consentRequestId);
 void linkRetentionJob(UUID rightsId,UUID jobId,long version);
}
