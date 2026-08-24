package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.domain.RecipientStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipientRepository {
    boolean canAccessActiveOrganization(UUID actorId, UUID organizationId);
    Optional<RecipientStoredRecord> findActiveByEmailHmac(UUID organizationId,
                                                           String emailLookupHmac,
                                                           UUID excludedRecipientId);
    void insert(RecipientStoredRecord record, UUID actorId, Instant now);
    Optional<RecipientStoredRecord> findScoped(UUID actorId, UUID recipientId);
    boolean update(RecipientStoredRecord record, UUID actorId, Instant now, long expectedVersion);
    List<RecipientStoredRecord> searchScoped(UUID actorId, String emailLookupHmac,
                                             RecipientStatus status);
}
