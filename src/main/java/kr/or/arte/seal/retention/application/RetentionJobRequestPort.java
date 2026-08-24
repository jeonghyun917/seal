package kr.or.arte.seal.retention.application;
import java.time.Instant;import java.util.UUID;
public interface RetentionJobRequestPort {UUID requestUrgent(UUID rightsRequestId,UUID consentRequestId,UUID recipientId,UUID actor,Instant now);boolean isConfirmed(UUID jobId,UUID actor);void placeRestrictionHold(UUID consentRequestId,UUID actor,Instant now);}
