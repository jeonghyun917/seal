package kr.or.arte.seal.consent.persistence;

import kr.or.arte.seal.consent.application.ConsentResponseItemRecord;
import kr.or.arte.seal.consent.application.ConsentResponseRecord;
import kr.or.arte.seal.consent.application.ConsentSubmissionResult;
import kr.or.arte.seal.consent.application.PublicConsentItem;
import kr.or.arte.seal.consent.application.PublicConsentRepository;
import kr.or.arte.seal.consent.application.PublicConsentSessionRecord;
import kr.or.arte.seal.consent.application.PublicConsentSessionView;
import kr.or.arte.seal.consent.application.PublicLinkRecord;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MyBatisPublicConsentRepository implements PublicConsentRepository {
    private final PublicConsentMapper mapper;

    public MyBatisPublicConsentRepository(PublicConsentMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<PublicLinkRecord> findLink(String tokenHmac) {
        return mapper.findLink(tokenHmac);
    }

    public Optional<PublicLinkRecord> findLinkForUpdate(String tokenHmac) {
        return mapper.findLinkForUpdate(tokenHmac);
    }

    public void markAccessed(UUID linkId, Instant now) {
        mapper.markAccessed(linkId, now);
    }

    public void insertSession(PublicConsentSessionRecord session) {
        mapper.insertSession(session);
    }

    public Optional<PublicConsentSessionView> findSessionForUpdate(String flowHmac, Instant now) {
        return mapper.findSessionForUpdate(flowHmac);
    }

    public List<PublicConsentItem> findItems(UUID requestId) {
        return mapper.findItems(requestId);
    }

    public Optional<ConsentSubmissionResult> findResponse(UUID requestId, UUID idempotencyKey) {
        return mapper.findResponse(requestId, idempotencyKey);
    }

    public void insertResponse(ConsentResponseRecord response,
            List<ConsentResponseItemRecord> items) {
        mapper.insertResponse(response);
        items.forEach(mapper::insertResponseItem);
    }

    public void complete(UUID requestId, UUID linkId, UUID sessionId, Instant now) {
        if (mapper.completeRequest(requestId, now) != 1) {
            throw new IllegalStateException("이미 완료되었거나 만료된 요청입니다.");
        }
        mapper.completeLink(linkId, now);
        mapper.completeSession(sessionId, now);
        mapper.expireOtherSessions(requestId, sessionId, now);
        mapper.insertCompletedEvent(requestId, now);
    }

    public void expireSessions(UUID requestId, Instant now) {
        mapper.expireSessions(requestId, now);
    }
    public int expireSession(UUID sessionId, Instant now) {
        return mapper.expireSession(sessionId, now);
    }

    public int expireDueSessions(Instant now, int limit) {
        return mapper.expireDueSessions(now, limit);
    }


    public Optional<PublicConsentSessionView> findSession(String flowHmac) {
        return mapper.findSession(flowHmac);
    }
}
