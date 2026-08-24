package kr.or.arte.seal.delivery.application;
import java.time.Instant;import java.util.UUID;
public record MailOutboxRecord(UUID outboxId,UUID requestId,UUID linkId,UUID organizationId,byte[] emailCiphertext,
 byte[] htmlCiphertext,String keyId,String subject,String htmlSha256,MailOutboxStatus status,Instant scheduledAt,
 Instant nextAttemptAt,int attemptCount,Instant lockedAt,long versionNo){public MailOutboxRecord{emailCiphertext=emailCiphertext.clone();htmlCiphertext=htmlCiphertext.clone();}@Override public byte[] emailCiphertext(){return emailCiphertext.clone();}@Override public byte[] htmlCiphertext(){return htmlCiphertext.clone();}}
