package kr.or.arte.seal.retention.application;
import kr.or.arte.seal.retention.domain.RetentionJobStatus;import java.time.*;import java.util.UUID;
public record RetentionJobSummary(UUID id,UUID consentRequestId,RetentionJobStatus status,LocalDate dueDate,String destructionMethod,Instant requestedAt){ }
