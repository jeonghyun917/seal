package kr.or.arte.seal.retention.application;
import kr.or.arte.seal.retention.domain.RetentionJobStatus;import java.time.*;import java.util.UUID;
public record RetentionJobRecord(UUID id,UUID consentRequestId,UUID recipientId,UUID policyVersionId,UUID sourceRightsRequestId,LocalDate anchorDate,LocalDate dueDate,RetentionJobStatus status,String destructionMethod,UUID idempotencyKey,UUID requestedBy,Instant requestedAt,UUID approvedBy,UUID executedBy,UUID confirmedBy,int destroyedMailCount,int destroyedFileCount,boolean recipientPiiDestroyed,long rowVersion){ }
