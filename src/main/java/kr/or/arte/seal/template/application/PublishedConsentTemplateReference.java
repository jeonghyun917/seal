package kr.or.arte.seal.template.application;

import java.util.UUID;

public record PublishedConsentTemplateReference(UUID templateId, UUID templateVersionId,
                                                UUID processingActivityId,
                                                UUID processingActivityVersionId,
                                                UUID retentionPolicyVersionId,
                                                String retentionDisplayText,
                                                String contentHash,
                                                PublishedTemplateView view) {
}
