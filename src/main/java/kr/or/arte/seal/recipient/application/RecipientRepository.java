package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.domain.RecipientStatus;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public interface RecipientRepository {
    boolean canAccessActiveOrganization(UUID actorId, UUID organizationId);
    Optional<RecipientStoredRecord> findActiveByEmailHmac(UUID organizationId,
                                                           String emailLookupHmac,
                                                           UUID excludedRecipientId);
    void insert(RecipientStoredRecord record, UUID actorId, Instant now);
    Optional<RecipientStoredRecord> findScoped(UUID actorId, UUID recipientId);
    default List<RecipientStoredRecord> findManyScoped(UUID actorId, List<UUID> recipientIds) {
        return recipientIds.stream().distinct().map(id -> findScoped(actorId,id)).flatMap(Optional::stream).toList();
    }
    boolean update(RecipientStoredRecord record, UUID actorId, Instant now, long expectedVersion);
    List<RecipientStoredRecord> searchScoped(UUID actorId, String emailLookupHmac,
                                             RecipientStatus status);
    default List<RecipientStoredRecord> searchScoped(UUID actorId, String emailLookupHmac,
            String collectionBasis, RecipientStatus status) {
        String keyword = collectionBasis == null ? "" : collectionBasis.strip().toLowerCase(Locale.ROOT);
        return searchScoped(actorId, emailLookupHmac, status).stream()
                .filter(row -> keyword.isEmpty() || (row.collectionBasis() != null
                        && row.collectionBasis().toLowerCase(Locale.ROOT).contains(keyword)))
                .toList();
    }
    default List<RecipientStoredRecord> searchScoped(UUID actorId, String emailLookupHmac,
            RecipientStatus status, int limit, int offset) {
        return searchScoped(actorId,emailLookupHmac,status).stream().skip(offset).limit(limit).toList();
    }
    default List<RecipientStoredRecord> searchScoped(UUID actorId, String emailLookupHmac,
            String collectionBasis, RecipientStatus status, int limit, int offset) {
        return searchScoped(actorId,emailLookupHmac,collectionBasis,status).stream()
                .skip(offset).limit(limit).toList();
    }
}
