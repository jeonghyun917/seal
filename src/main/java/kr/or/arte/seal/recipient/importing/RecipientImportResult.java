package kr.or.arte.seal.recipient.importing;
import java.util.UUID;
public record RecipientImportResult(UUID batchId,int importedCount,boolean existing) {}
