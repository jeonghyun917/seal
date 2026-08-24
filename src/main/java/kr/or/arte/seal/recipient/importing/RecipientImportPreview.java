package kr.or.arte.seal.recipient.importing;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record RecipientImportPreview(UUID batchId,RecipientImportStatus status,MalwareScanResult malwareScanStatus,
        int totalCount,int validCount,int duplicateCount,int errorCount,int importedCount,Instant expiresAt,
        List<RecipientImportRowView> rows){public RecipientImportPreview{rows=List.copyOf(rows);}}
