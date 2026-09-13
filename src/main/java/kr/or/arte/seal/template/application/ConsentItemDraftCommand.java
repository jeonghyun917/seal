package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.domain.ConsentItemType;

import java.util.List;
import java.util.UUID;

public record ConsentItemDraftCommand(
        ConsentItemType type,
        String title,
        boolean required,
        String purpose,
        String retentionText,
        String refusalNotice,
        String thirdPartyRecipient,
        List<UUID> processingDataItemIds
) {
}
