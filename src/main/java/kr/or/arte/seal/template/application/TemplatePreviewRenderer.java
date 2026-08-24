package kr.or.arte.seal.template.application;

import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityReference;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TemplatePreviewRenderer {
    public PublishedTemplateView toView(ConsentTemplateDetail detail,
                                        ApprovedProcessingActivityReference policy) {
        Map<UUID, String> dataItemNames = policy.dataItems().stream()
                .collect(Collectors.toMap(item -> item.processingDataItemId(),
                        item -> item.itemName()));
        var items = detail.version().items().stream()
                .sorted(Comparator.comparingInt(item -> item.order()))
                .map(item -> new PublishedTemplateView.Item(item.order(), item.code(), item.type(),
                        item.title(), item.required(), item.purpose(), item.retentionText(),
                        item.refusalNotice(), item.thirdPartyRecipient(),
                        item.processingDataItemIds().stream()
                                .sorted(Comparator.comparingInt(id -> policy.dataItems().stream()
                                        .filter(reference -> reference.processingDataItemId().equals(id))
                                        .map(reference -> reference.itemOrder()).findFirst()
                                        .orElse(Integer.MAX_VALUE)))
                                .map(dataItemNames::get).filter(java.util.Objects::nonNull).toList()))
                .toList();
        return new PublishedTemplateView(detail.version().title(),
                detail.version().introductionHtml(), detail.processingActivityName(),
                policy.processingPurpose(), items);
    }
}
