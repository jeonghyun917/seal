package kr.or.arte.seal.recipient.importing;

import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.Instant; import java.util.List; import java.util.Optional; import java.util.UUID;

@Mapper
public interface RecipientImportMapper {
    boolean canAccessOrganization(@Param("actorId") UUID actorId,@Param("organizationId") UUID organizationId);
    boolean activeEmailExists(@Param("organizationId") UUID organizationId,@Param("emailHmac") String emailHmac);
    void insertBatch(@Param("batch") RecipientImportBatchRecord batch);
    void insertRow(@Param("row") RecipientImportRowRecord row);
    Optional<RecipientImportBatchRecord> findScoped(@Param("actorId") UUID actorId,@Param("batchId") UUID batchId);
    Optional<RecipientImportBatchRecord> findScopedForUpdate(@Param("actorId") UUID actorId,@Param("batchId") UUID batchId);
    List<RecipientImportRowRecord> findRows(@Param("batchId") UUID batchId);
    void insertRecipientFromImport(@Param("recipient") RecipientStoredRecord recipient,@Param("actorId") UUID actorId,@Param("now") Instant now);
    void markImported(@Param("batchId") UUID batchId,@Param("rowNumber") int rowNumber,@Param("recipientId") UUID recipientId);
    void purgeRemaining(@Param("batchId") UUID batchId);
    void complete(@Param("batchId") UUID batchId,@Param("importedCount") int importedCount,@Param("now") Instant now);
    int expireDue(@Param("now") Instant now,@Param("limit") int limit);
}
