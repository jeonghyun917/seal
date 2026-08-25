package kr.or.arte.seal.consent.application;

import kr.or.arte.seal.link.application.SecureLinkHasher; import kr.or.arte.seal.link.application.SecureTokenGenerator;
import kr.or.arte.seal.link.domain.SecureLinkStatus; import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.UUID;

@Service public class PublicConsentEntryService {
 private final PublicConsentRepository repository;private final SecureLinkHasher hasher;private final SecureTokenGenerator generator;private final Clock clock;
 public PublicConsentEntryService(PublicConsentRepository r,SecureLinkHasher h,SecureTokenGenerator g,Clock c){repository=r;hasher=h;generator=g;clock=c;}
 @Transactional(readOnly=true) public PublicConsentEntry inspect(String token){if(!validToken(token))return new PublicConsentEntry(PublicConsentEntryStatus.INVALID);return repository.findLinkCandidates(hasher.candidates(token)).map(this::entry).orElse(new PublicConsentEntry(PublicConsentEntryStatus.INVALID));}
 @Transactional public PublicConsentFlow start(String token){if(!validToken(token))throw new IllegalArgumentException("유효하지 않은 링크입니다.");var row=repository.findLinkForUpdateCandidates(hasher.candidates(token)).orElseThrow(()->new IllegalArgumentException("유효하지 않은 링크입니다."));
  if(entry(row).status()!=PublicConsentEntryStatus.VALID)throw new IllegalArgumentException("사용할 수 없는 링크입니다.");Instant now=clock.instant();if(row.linkStatus()==SecureLinkStatus.ISSUED)repository.markAccessed(row.linkId(),now);
  String flow=generator.generate();Instant expires=now.plus(Duration.ofMinutes(30));repository.insertSession(new PublicConsentSessionRecord(UUID.randomUUID(),hasher.hmac("flow:"+flow),row.requestId(),row.linkId(),expires,now));return new PublicConsentFlow(flow,row.requestId(),expires);}
 private PublicConsentEntry entry(PublicLinkRecord r){if(r.linkStatus()==SecureLinkStatus.COMPLETED||r.requestStatus()==ConsentRequestStatus.COMPLETED)return new PublicConsentEntry(PublicConsentEntryStatus.COMPLETED);if(!r.expiresAt().isAfter(clock.instant())||r.linkStatus()==SecureLinkStatus.EXPIRED)return new PublicConsentEntry(PublicConsentEntryStatus.EXPIRED);if((r.linkStatus()==SecureLinkStatus.ISSUED||r.linkStatus()==SecureLinkStatus.ACCESSED)&&r.requestStatus()==ConsentRequestStatus.ACTIVE)return new PublicConsentEntry(PublicConsentEntryStatus.VALID);return new PublicConsentEntry(PublicConsentEntryStatus.INVALID);}
 private static boolean validToken(String token){return token!=null&&token.matches("[A-Za-z0-9_-]{43}");}
}
