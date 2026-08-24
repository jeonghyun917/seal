package kr.or.arte.seal.consent.persistence;
import kr.or.arte.seal.consent.application.*;import org.springframework.stereotype.Repository;
import java.time.Instant;import java.util.*;
@Repository public class MyBatisPublicConsentRepository implements PublicConsentRepository{
 private final PublicConsentMapper mapper;public MyBatisPublicConsentRepository(PublicConsentMapper m){mapper=m;}
 public Optional<PublicLinkRecord> findLink(String h){return mapper.findLink(h);}public Optional<PublicLinkRecord> findLinkForUpdate(String h){return mapper.findLinkForUpdate(h);}public void markAccessed(UUID i,Instant n){mapper.markAccessed(i,n);}public void insertSession(PublicConsentSessionRecord s){mapper.insertSession(s);}
 public Optional<PublicConsentSessionView> findSessionForUpdate(String h,Instant n){return mapper.findSessionForUpdate(h);}public List<PublicConsentItem> findItems(UUID r){return mapper.findItems(r);}public Optional<ConsentSubmissionResult> findResponse(UUID r,UUID i){return mapper.findResponse(r,i);}public void insertResponse(ConsentResponseRecord r,List<ConsentResponseItemRecord> items){mapper.insertResponse(r);items.forEach(i->mapper.insertResponseItem(i));}
 public void complete(UUID r,UUID l,UUID s,Instant n){if(mapper.completeRequest(r,n)!=1)throw new IllegalStateException("이미 완료되었거나 만료된 요청입니다.");mapper.completeLink(l,n);mapper.completeSession(s,n);mapper.insertCompletedEvent(r,n);}
 public Optional<PublicConsentSessionView> findSession(String h){return mapper.findSession(h);}
}
