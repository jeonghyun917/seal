package kr.or.arte.seal.request.application;

import kr.or.arte.seal.link.application.SecureLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class ConsentRequestExpiryService {
    private final ConsentRequestRepository requests;
    private final SecureLinkRepository links;
    private final PublicConsentSessionLifecycle sessions;
    private final Clock clock;
    public ConsentRequestExpiryService(ConsentRequestRepository requests, SecureLinkRepository links,
            PublicConsentSessionLifecycle sessions, Clock clock) {
        this.requests=requests; this.links=links; this.sessions=sessions; this.clock=clock;
    }
    @Transactional
    public int expireDue(int limit) {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("만료 배치 크기는 1~1000이어야 합니다.");
        int changed=0;
        for (var request : requests.findDueForUpdate(clock.instant(), limit)) {
            links.expireCurrent(request.requestId(), clock.instant());
            if (requests.markExpired(request.requestId(), clock.instant(), request.versionNo())) {
                sessions.expireSessions(request.requestId(), clock.instant());
                requests.insertEvent(request.requestId(), "EXPIRED", "SYSTEM", null, "TIME_LIMIT", clock.instant());
                changed++;
            }
        }
        return changed;
    }
}
