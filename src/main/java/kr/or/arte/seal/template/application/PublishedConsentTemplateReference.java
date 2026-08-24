package kr.or.arte.seal.template.application;

import java.util.UUID;
import java.util.List;
import java.util.Map;

public record PublishedConsentTemplateReference(UUID templateId, UUID templateVersionId,
                                                UUID processingActivityId,
                                                UUID processingActivityVersionId,
                                                UUID retentionPolicyVersionId,
                                                String retentionDisplayText,
                                                String contentHash,
                                                PublishedTemplateView view,
                                                Map<Integer, List<UUID>> dataItemIdsByItemOrder) {
    public PublishedConsentTemplateReference {
        dataItemIdsByItemOrder = dataItemIdsByItemOrder.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())));
    }
}
