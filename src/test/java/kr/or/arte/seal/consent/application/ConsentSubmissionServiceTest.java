package kr.or.arte.seal.consent.application;

import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import org.junit.jupiter.api.Test;
import java.time.*; import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class ConsentSubmissionServiceTest {
 @Test void completesAllItemsOnceAndReturnsExistingForSameIdempotencyKey(){
  FakeRepository repo=new FakeRepository();byte[] key=new byte[32];Arrays.fill(key,(byte)8);
  var hasher=new kr.or.arte.seal.link.application.SecureLinkHasher(key);String flow="flow";repo.flowHash=hasher.hmac("flow:"+flow);
  var queued=new java.util.ArrayList<UUID>();
  var service=new ConsentSubmissionService(repo,hasher,command->{},(responseId,requestId,at)->queued.add(responseId),Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"),ZoneOffset.UTC));
  UUID idem=UUID.randomUUID();Map<UUID,ConsentDecision> decisions=Map.of(repo.items.get(0).itemId(),ConsentDecision.AGREE,repo.items.get(1).itemId(),ConsentDecision.DISAGREE);
  var first=service.submit(flow,decisions,idem);var second=service.submit(flow,decisions,idem);
  assertThat(first.existing()).isFalse();assertThat(second.responseId()).isEqualTo(first.responseId());assertThat(second.existing()).isTrue();assertThat(repo.completed).isTrue();assertThat(queued).containsExactly(first.responseId());
 }
 @Test void rejectsAnotherIdempotencyKeyAfterCompletion(){FakeRepository repo=new FakeRepository();byte[] key=new byte[32];Arrays.fill(key,(byte)8);var hasher=new kr.or.arte.seal.link.application.SecureLinkHasher(key);String flow="flow";repo.flowHash=hasher.hmac("flow:"+flow);var service=new ConsentSubmissionService(repo,hasher,command->{},(responseId,requestId,at)->{},Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"),ZoneOffset.UTC));Map<UUID,ConsentDecision> decisions=Map.of(repo.items.get(0).itemId(),ConsentDecision.AGREE,repo.items.get(1).itemId(),ConsentDecision.AGREE);service.submit(flow,decisions,UUID.randomUUID());org.assertj.core.api.Assertions.assertThatThrownBy(()->service.submit(flow,decisions,UUID.randomUUID())).isInstanceOf(IllegalStateException.class);}
 private static final class FakeRepository implements PublicConsentRepository{
  final UUID request=UUID.randomUUID(),link=UUID.randomUUID(),session=UUID.randomUUID();String flowHash;boolean completed;ConsentSubmissionResult existing;UUID existingKey;
  final List<PublicConsentItem> items=List.of(new PublicConsentItem(UUID.randomUUID(),1,"필수 수집",true,"목적","3년","거부 가능","이름",""),new PublicConsentItem(UUID.randomUUID(),2,"선택 홍보",false,"안내","1년","거부 가능","이메일","제공사"));
  public Optional<PublicLinkRecord> findLink(String h){return Optional.empty();}public Optional<PublicLinkRecord> findLinkForUpdate(String h){return Optional.empty();}public void markAccessed(UUID i,Instant n){}public void insertSession(PublicConsentSessionRecord s){}
  public Optional<PublicConsentSessionView> findSessionForUpdate(String h,Instant n){return h.equals(flowHash)?Optional.of(new PublicConsentSessionView(session,request,link,completed?"COMPLETED":"ACTIVE",n.plusSeconds(60),"{}","a".repeat(64),completed?ConsentRequestStatus.COMPLETED:ConsentRequestStatus.ACTIVE)):Optional.empty();}
  public List<PublicConsentItem> findItems(UUID r){return items;}public Optional<ConsentSubmissionResult> findResponse(UUID r,UUID i){return Objects.equals(i,existingKey)?Optional.ofNullable(existing):Optional.empty();}
  public void insertResponse(ConsentResponseRecord r,List<ConsentResponseItemRecord> values){existingKey=r.idempotencyKey();existing=new ConsentSubmissionResult(r.responseId(),r.status(),false,r.submittedAt());}
  public void complete(UUID r,UUID l,UUID s,Instant n){completed=true;}
  public void expireSessions(UUID requestId,Instant now){}
 }
}
