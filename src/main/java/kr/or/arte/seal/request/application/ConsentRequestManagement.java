package kr.or.arte.seal.request.application;

import java.util.UUID;

public interface ConsentRequestManagement {
    ConsentRequestCreateResult create(UUID actorId, CreateConsentRequestCommand command);
    void updateDraft(UUID actorId, UUID requestId, UpdateConsentRequestCommand command);
    void finalizeRequest(UUID actorId, UUID requestId, long versionNo);
    void cancel(UUID actorId, UUID requestId, long versionNo, String reason);
}
