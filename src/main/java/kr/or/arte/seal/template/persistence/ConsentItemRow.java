package kr.or.arte.seal.template.persistence;

import kr.or.arte.seal.template.domain.ConsentItemType;

import java.util.UUID;

public record ConsentItemRow(
        UUID itemId,
        int itemOrder,
        String itemCode,
        ConsentItemType itemType,
        String itemTitle,
        boolean required,
        String purposeText,
        String retentionText,
        String refusalNotice,
        String thirdPartyRecipient
) {
}
