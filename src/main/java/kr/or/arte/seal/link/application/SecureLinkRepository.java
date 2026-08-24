package kr.or.arte.seal.link.application;

import java.time.Instant;
import java.util.UUID;

public interface SecureLinkRepository {
    void insert(SecureLinkInsert value);
    void revokeCurrent(UUID requestId, UUID actorId, Instant now, String reason);
    boolean activateRequest(UUID requestId, UUID linkId, UUID actorId, Instant now, long versionNo);
    boolean replaceCurrentLink(UUID requestId, UUID linkId, UUID actorId, Instant now, long versionNo);
}
