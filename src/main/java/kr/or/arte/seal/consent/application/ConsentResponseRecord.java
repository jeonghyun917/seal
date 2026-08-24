package kr.or.arte.seal.consent.application;
import java.time.Instant; import java.util.UUID;
public record ConsentResponseRecord(UUID responseId,UUID requestId,UUID sessionId,ConsentResponseStatus status,UUID idempotencyKey,String snapshotHash,Instant submittedAt) {}
