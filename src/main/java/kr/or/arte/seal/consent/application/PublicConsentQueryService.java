package kr.or.arte.seal.consent.application;
import com.fasterxml.jackson.databind.ObjectMapper;import kr.or.arte.seal.link.application.SecureLinkHasher;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;import java.time.Instant;
@Service public class PublicConsentQueryService{
 private final PublicConsentRepository repository;private final SecureLinkHasher hasher;private final ObjectMapper json;private final Clock clock;
 public PublicConsentQueryService(PublicConsentRepository r,SecureLinkHasher h,ObjectMapper j,Clock c){repository=r;hasher=h;json=j;clock=c;}
 @Transactional public PublicConsentPage load(String flow){
  Instant now=clock.instant();
  var s=repository.findSessionForUpdateCandidates(hasher.candidates("flow:"+flow),now)
    .orElseThrow(()->new IllegalArgumentException("동의 세션을 찾을 수 없습니다."));
  if(!s.expiresAt().isAfter(now)&&!"COMPLETED".equals(s.sessionStatus())){
   repository.expireSession(s.sessionId(),now);
   throw new IllegalArgumentException("동의 세션이 만료되었습니다.");
  }
  try{var n=json.readTree(s.snapshotJson());return new PublicConsentPage(n.path("institution").asText(),n.path("title").asText(),n.path("introductionHtml").asText(),n.path("processingActivityName").asText(),n.path("processingPurpose").asText(),n.path("retentionDisplayText").asText(),n.path("withdrawalNotice").asText(),Instant.parse(n.path("expiresAt").asText()),repository.findItems(s.requestId()));}
  catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalStateException("동의 내용을 표시할 수 없습니다.",e);}
 }
}
