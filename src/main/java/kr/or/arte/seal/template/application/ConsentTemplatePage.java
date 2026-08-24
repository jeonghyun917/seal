package kr.or.arte.seal.template.application;

import java.util.List;
import java.util.UUID;

public record ConsentTemplatePage(
        UUID consentTemplateId,
        String templateCode,
        String templateName,
        UUID processingActivityId,
        String processingActivityName,
        String templateStatus,
        UUID currentPublishedVersionId,
        long versionNo,
        List<ConsentTemplateVersionSummary> versions
) {
    public ConsentTemplatePage {
        versions = List.copyOf(versions);
    }
}
