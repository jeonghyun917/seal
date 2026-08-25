package kr.or.arte.seal.consent.application;

import kr.or.arte.seal.link.application.SecureLinkHasher; import kr.or.arte.seal.link.application.SecureTokenGenerator;
import kr.or.arte.seal.link.domain.SecureLinkStatus; import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;
import java.security.SecureRandom; import java.time.*; import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class PublicConsentEntryServiceTest {
 @Test void inspectIsReadOnlyAndStartMarksAccessed(){
  FakeRepository repo=new FakeRepository(); byte[] key=new byte[32];Arrays.fill(key,(byte)3);
  String token="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";repo.row=new PublicLinkRecord(UUID.randomUUID(),UUID.randomUUID(),SecureLinkStatus.ISSUED,
    Instant.parse("2026-08-25T00:00:00Z"),ConsentRequestStatus.ACTIVE);
  var service=new PublicConsentEntryService(repo,new SecureLinkHasher(key),new SecureTokenGenerator(new SecureRandom()),
    Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"),ZoneOffset.UTC));
  repo.expectedHash=new SecureLinkHasher(key).hmac(token);
  assertThat(service.inspect(token).status()).isEqualTo(PublicConsentEntryStatus.VALID);
  assertThat(repo.accessed).isFalse();
  assertThat(service.start(token).flowId()).isNotBlank();
  assertThat(repo.accessed).isTrue();
 }
 @Test void malformedAndExpiredTokensAreNotUsable(){FakeRepository repo=new FakeRepository();byte[] key=new byte[32];Arrays.fill(key,(byte)3);var hasher=new SecureLinkHasher(key);var service=new PublicConsentEntryService(repo,hasher,new SecureTokenGenerator(new SecureRandom()),Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"),ZoneOffset.UTC));assertThat(service.inspect("bad").status()).isEqualTo(PublicConsentEntryStatus.INVALID);assertThat(repo.findCalls).isZero();String token="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";repo.expectedHash=hasher.hmac(token);repo.row=new PublicLinkRecord(UUID.randomUUID(),UUID.randomUUID(),SecureLinkStatus.ISSUED,Instant.parse("2026-08-23T00:00:00Z"),ConsentRequestStatus.ACTIVE);assertThat(service.inspect(token).status()).isEqualTo(PublicConsentEntryStatus.EXPIRED);}
 private static final class FakeRepository implements PublicConsentRepository{
  PublicLinkRecord row;String expectedHash;boolean accessed;int findCalls;
  public Optional<PublicLinkRecord> findLink(String h){findCalls++;return h.equals(expectedHash)?Optional.of(row):Optional.empty();}
  public Optional<PublicLinkRecord> findLinkForUpdate(String h){return findLink(h);}
  public void markAccessed(UUID id,Instant now){accessed=true;}
  public void insertSession(PublicConsentSessionRecord s){}
  public void expireSessions(UUID requestId,Instant now){}
 }
}
