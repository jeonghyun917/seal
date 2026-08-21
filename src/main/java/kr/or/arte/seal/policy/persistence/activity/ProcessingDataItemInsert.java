package kr.or.arte.seal.policy.persistence.activity;

import java.util.UUID;

public record ProcessingDataItemInsert(UUID processingDataItemId,
        UUID processingActivityVersionId, int itemOrder, String itemName,
        String necessityReason, boolean required) {
}
