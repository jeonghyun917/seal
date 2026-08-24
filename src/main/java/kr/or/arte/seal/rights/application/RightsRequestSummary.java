package kr.or.arte.seal.rights.application;
import kr.or.arte.seal.rights.domain.*;import java.time.*;import java.util.UUID;
public record RightsRequestSummary(UUID id,String code,RightsRequestType type,RightsRequestStatus status,LocalDate receivedDate,LocalDate dueDate,Instant createdAt){ }
