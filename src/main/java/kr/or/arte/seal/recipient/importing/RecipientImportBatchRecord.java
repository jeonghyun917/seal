package kr.or.arte.seal.recipient.importing;

import java.time.Instant;
import java.util.UUID;

public record RecipientImportBatchRecord(UUID batchId, UUID organizationUnitId, String fileSha256,
        MalwareScanResult malwareScanStatus, String collectionBasis, RecipientImportStatus status,
        int totalCount, int validCount, int duplicateCount, int errorCount, int importedCount,
        Instant expiresAt, UUID createdBy, Instant createdAt, Instant completedAt, long versionNo) {
    public RecipientImportBatchRecord completed(int count, Instant now) {
        return new RecipientImportBatchRecord(batchId,organizationUnitId,fileSha256,malwareScanStatus,
                collectionBasis,RecipientImportStatus.IMPORTED,totalCount,validCount,duplicateCount,
                errorCount,count,expiresAt,createdBy,createdAt,now,versionNo+1);
    }
}
