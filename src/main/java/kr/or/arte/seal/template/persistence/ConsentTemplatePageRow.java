package kr.or.arte.seal.template.persistence;

import java.util.UUID;

public record ConsentTemplatePageRow(
        UUID consentTemplateId,
        String templateCode,
        String templateName,
        UUID processingActivityId,
        String processingActivityName,
        String templateStatus,
        UUID currentPublishedVersionId,
        long versionNo
) {
}
