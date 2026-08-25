package kr.or.arte.seal.consent.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class PublicConsentSessionExpiryService {
    private final PublicConsentRepository repository;
    private final Clock clock;

    public PublicConsentSessionExpiryService(PublicConsentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public int expireDue(int limit) {
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("만료 배치 크기는 1~1000이어야 합니다.");
        }
        return repository.expireDueSessions(clock.instant(), limit);
    }
}
