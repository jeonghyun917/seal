package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.domain.ConsentItemType;

import java.util.List;

public record PublishedTemplateView(
        String title,
        String introductionHtml,
        String processingActivityName,
        String processingPurpose,
        List<Item> items
) {
    public PublishedTemplateView {
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
            List<String> dataItemNames
    ) {
        public Item {
            dataItemNames = List.copyOf(dataItemNames);
        }
    }
}
