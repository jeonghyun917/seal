package kr.or.arte.seal.policy.application.activity;

import java.util.UUID;

public record ApprovedDataItemReference(
        UUID processingDataItemId,
        int itemOrder,
        String itemName,
        boolean required
) {
}
