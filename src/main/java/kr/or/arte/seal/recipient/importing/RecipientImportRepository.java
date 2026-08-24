package kr.or.arte.seal.recipient.importing;

import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipientImportRepository {
    boolean canAccessOrganization(UUID actorId,UUID organizationId);
    boolean activeEmailExists(UUID organizationId,String emailHmac);
    void insertBatch(RecipientImportBatchRecord batch,List<RecipientImportRowRecord> rows);
    default Optional<RecipientImportBatchRecord> findScoped(UUID actorId,UUID batchId){return findScopedForUpdate(actorId,batchId);}
    Optional<RecipientImportBatchRecord> findScopedForUpdate(UUID actorId,UUID batchId);
    List<RecipientImportRowRecord> findRows(UUID batchId);
    UUID insertRecipientFromImport(RecipientStoredRecord recipient,UUID actorId,Instant now);
    void markImported(UUID batchId,int rowNumber,UUID recipientId);
    void complete(UUID batchId,int importedCount,Instant now);
    int expireDue(Instant now,int limit);
}
