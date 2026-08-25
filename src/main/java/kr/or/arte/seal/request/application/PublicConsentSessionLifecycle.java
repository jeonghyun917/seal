package kr.or.arte.seal.request.application;

import java.time.Instant;
import java.util.UUID;

public interface PublicConsentSessionLifecycle {
    void expireSessions(UUID requestId, Instant now);
}
