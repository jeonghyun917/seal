package kr.or.arte.seal.policy.application.activity;

import java.util.UUID;

public record ProcessingDataItemView(UUID processingActivityVersionId, int itemOrder,
        String itemName, String necessityReason, boolean required) {
}
