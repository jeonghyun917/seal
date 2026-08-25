package kr.or.arte.seal.consent.application;

import kr.or.arte.seal.audit.application.*; import kr.or.arte.seal.link.application.SecureLinkHasher; import kr.or.arte.seal.evidence.application.EvidenceQueue;
import kr.or.arte.seal.request.domain.ConsentRequestStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.*;

@Service public class ConsentSubmissionService {
 private final PublicConsentRepository repository;private final SecureLinkHasher hasher;private final AuditEventWriter audit;private final EvidenceQueue evidenceQueue;private final Clock clock;
 public ConsentSubmissionService(PublicConsentRepository r,SecureLinkHasher h,AuditEventWriter a,EvidenceQueue q,Clock c){repository=r;hasher=h;audit=a;evidenceQueue=q;clock=c;}
 @Transactional public ConsentSubmissionResult submit(String flowId,Map<UUID,ConsentDecision> decisions,UUID idempotencyKey){if(idempotencyKey==null)throw new IllegalArgumentException("제출 멱등키가 필요합니다.");Instant now=clock.instant();
  var session=repository.findSessionForUpdateCandidates(hasher.candidates("flow:"+flowId),now).orElseThrow(()->new IllegalArgumentException("동의 세션이 만료되었습니다."));
  var previous=repository.findResponse(session.requestId(),idempotencyKey);if(previous.isPresent()){var p=previous.get();return new ConsentSubmissionResult(p.responseId(),p.status(),true);}
  if("ACTIVE".equals(session.sessionStatus())&&!session.expiresAt().isAfter(now))repository.expireSession(session.sessionId(),now);
  if(!"ACTIVE".equals(session.sessionStatus())||!session.expiresAt().isAfter(now)||session.requestStatus()!=ConsentRequestStatus.ACTIVE)throw new IllegalStateException("이미 완료되었거나 만료된 요청입니다.");
  var items=repository.findItems(session.requestId());ConsentDecisionValidator.validate(items.stream().map(PublicConsentItem::itemId).toList(),decisions);
  UUID responseId=UUID.randomUUID();ConsentResponseStatus status=decisions.values().stream().allMatch(d->d==ConsentDecision.DISAGREE)?ConsentResponseStatus.DECLINED_ALL:ConsentResponseStatus.SUBMITTED;
  var response=new ConsentResponseRecord(responseId,session.requestId(),session.sessionId(),status,idempotencyKey,session.snapshotHash(),now);
  var values=items.stream().map(i->new ConsentResponseItemRecord(responseId,i.itemId(),decisions.get(i.itemId()),i.order())).toList();repository.insertResponse(response,values);evidenceQueue.enqueue(responseId,session.requestId(),now);repository.complete(session.requestId(),session.linkId(),session.sessionId(),now);
  audit.append(new AuditEventCommand(now,"PUBLIC",null,"CONSENT_SUBMIT","CONSENT_REQUEST",session.requestId().toString(),"SUCCESS",status.name(),UUID.randomUUID(),Map.of("itemCount",items.size())));
  return new ConsentSubmissionResult(responseId,status,false);}
}
