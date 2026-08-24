package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.domain.ConsentItemType;

import java.util.List;
import java.util.UUID;

public record TemplateContentHashPayload(
        String templateCode,
        int versionNumber,
        UUID processingActivityVersionId,
        String title,
        String introductionHtml,
        List<Item> items
) {
    public TemplateContentHashPayload {
        items = List.copyOf(items);
    }

    public record Item(
            int order,
            String code,
            ConsentItemType type,
            String title,
            boolean required,
            String purpose,
            String retentionText,
            String refusalNotice,
            String thirdPartyRecipient,
            List<DataItem> dataItems
    ) {
        public Item {
            dataItems = List.copyOf(dataItems);
        }
    }

    public record DataItem(UUID id, int order, String name) {
    }
}
