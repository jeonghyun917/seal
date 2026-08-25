package kr.or.arte.seal.rights.application;

import kr.or.arte.seal.rights.domain.*;import java.time.*;import java.util.UUID;
public record RightsRequestRecord(UUID id,String code,UUID organizationId,UUID recipientId,UUID consentRequestId,
 RightsRequestType type,RightsRequestStatus status,LocalDate receivedDate,LocalDate dueDate,byte[] detailCiphertext,
 byte[] verificationNoteCiphertext,byte[] resultNoteCiphertext,String keyId,String verificationNoteKeyId,
 String resultNoteKeyId,String verificationChannel,UUID createdBy,Instant createdAt,UUID decidedBy,UUID retentionJobId,long rowVersion){ }
