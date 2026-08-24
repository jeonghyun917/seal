package kr.or.arte.seal.delivery.application;

import kr.or.arte.seal.recipient.crypto.*;import org.junit.jupiter.api.Test;
import javax.crypto.spec.SecretKeySpec;import java.security.SecureRandom;import java.time.*;import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class MailOutboxWorkerTest{
 @Test void temporaryFailureSchedulesFirstRetryAfterOneMinute(){byte[] key=new byte[32];Arrays.fill(key,(byte)9);var cipher=new AesGcmPersonalDataCipher(Map.of("v1",new SecretKeySpec(key,"AES")),"v1",new SecureRandom());var repo=new FakeRepo(cipher);var worker=new MailOutboxWorker(repo,message->{throw new MailTransportException(true,"SMTP_TEMP");},cipher,true,Clock.fixed(repo.now,ZoneOffset.UTC));
  worker.processBatch(10);assertThat(repo.retryAt).isEqualTo(repo.now.plus(Duration.ofMinutes(1)));assertThat(repo.status).isEqualTo(MailOutboxStatus.RETRY_SCHEDULED);
 }
 @Test void disabledWorkerDoesNotClaimOrSend(){byte[] key=new byte[32];Arrays.fill(key,(byte)9);var cipher=new AesGcmPersonalDataCipher(Map.of("v1",new SecretKeySpec(key,"AES")),"v1",new SecureRandom());var repo=new FakeRepo(cipher);var worker=new MailOutboxWorker(repo,message->{throw new AssertionError("should not send");},cipher,false,Clock.fixed(repo.now,ZoneOffset.UTC));
  assertThat(worker.processBatch(10)).isZero();assertThat(repo.claimed).isFalse();
 }
 @Test void permanentFailureDoesNotRetry(){byte[] key=new byte[32];Arrays.fill(key,(byte)9);var cipher=new AesGcmPersonalDataCipher(Map.of("v1",new SecretKeySpec(key,"AES")),"v1",new SecureRandom());var repo=new FakeRepo(cipher);var worker=new MailOutboxWorker(repo,message->{throw new MailTransportException(false,"MAIL_INVALID");},cipher,true,Clock.fixed(repo.now,ZoneOffset.UTC));
  worker.processBatch(10);assertThat(repo.status).isEqualTo(MailOutboxStatus.SEND_FAILED);assertThat(repo.retryAt).isNull();
 }
 @Test void secondAndThirdTemporaryFailuresUseFiveAndThirtyMinuteDelays(){byte[] key=new byte[32];Arrays.fill(key,(byte)9);var cipher=new AesGcmPersonalDataCipher(Map.of("v1",new SecretKeySpec(key,"AES")),"v1",new SecureRandom());
  var second=new FakeRepo(cipher,1);new MailOutboxWorker(second,message->{throw new MailTransportException(true,"SMTP_TEMP");},cipher,true,Clock.fixed(second.now,ZoneOffset.UTC)).processBatch(10);assertThat(second.retryAt).isEqualTo(second.now.plus(Duration.ofMinutes(5)));
  var third=new FakeRepo(cipher,2);new MailOutboxWorker(third,message->{throw new MailTransportException(true,"SMTP_TEMP");},cipher,true,Clock.fixed(third.now,ZoneOffset.UTC)).processBatch(10);assertThat(third.retryAt).isEqualTo(third.now.plus(Duration.ofMinutes(30)));
 }
 @Test void claimsStaleProcessingWithTenMinuteLeaseAndRecordsSuccess(){byte[] key=new byte[32];Arrays.fill(key,(byte)9);var cipher=new AesGcmPersonalDataCipher(Map.of("v1",new SecretKeySpec(key,"AES")),"v1",new SecureRandom());var repo=new FakeRepo(cipher);new MailOutboxWorker(repo,message->{},cipher,true,Clock.fixed(repo.now,ZoneOffset.UTC)).processBatch(10);assertThat(repo.leaseExpiredBefore).isEqualTo(repo.now.minus(Duration.ofMinutes(10)));assertThat(repo.status).isEqualTo(MailOutboxStatus.SMTP_ACCEPTED);
 }
 private static final class FakeRepo implements MailOutboxRepository{final Instant now=Instant.parse("2026-08-24T00:00:00Z");final UUID id=UUID.randomUUID(),org=UUID.randomUUID();final MailOutboxRecord row;Instant retryAt;MailOutboxStatus status;
  FakeRepo(PersonalDataCipher c){this(c,0);}FakeRepo(PersonalDataCipher c,int attempt){var email=c.encrypt(new PersonalDataContext("MAIL_OUTBOX",id,org,"recipientEmail"),"a@example.com");var html=c.encrypt(new PersonalDataContext("MAIL_OUTBOX",id,org,"html"),"<p>hello</p>");row=new MailOutboxRecord(id,UUID.randomUUID(),UUID.randomUUID(),org,email.bytes(),html.bytes(),email.keyId(),"subject","a".repeat(64),MailOutboxStatus.QUEUED,now,now,attempt,null,0);}
  boolean claimed;Instant leaseExpiredBefore;public List<MailOutboxRecord> claimDue(Instant n,int l,Instant lease){claimed=true;leaseExpiredBefore=lease;return List.of(row);}public void accepted(UUID id,int attempt,Instant n){status=MailOutboxStatus.SMTP_ACCEPTED;}public void retry(UUID id,int attempt,Instant next,String code,Instant n){status=MailOutboxStatus.RETRY_SCHEDULED;retryAt=next;}public void failed(UUID id,int attempt,String code,Instant n){status=MailOutboxStatus.SEND_FAILED;}
 }
}
