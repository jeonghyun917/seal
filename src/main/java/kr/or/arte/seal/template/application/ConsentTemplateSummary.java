package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.domain.TemplateWorkflowStatus;

import java.util.UUID;

public record ConsentTemplateSummary(
        UUID consentTemplateId,
        String templateCode,
        String templateName,
        String processingActivityName,
        String templateStatus,
        Integer publishedVersionNumber,
        TemplateWorkflowStatus latestWorkflowStatus
) {
}
