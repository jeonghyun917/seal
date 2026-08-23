package kr.or.arte.seal.template.persistence;

import kr.or.arte.seal.template.domain.TemplateWorkflowStatus;

import java.util.UUID;

public record ConsentTemplateHeaderRow(
        UUID consentTemplateId,
        String templateCode,
        String templateName,
        UUID processingActivityId,
        String processingActivityName,
        String templateStatus,
        UUID currentPublishedVersionId,
        long templateVersionNo,
        UUID consentTemplateVersionId,
        UUID processingActivityVersionId,
        int versionNumber,
        TemplateWorkflowStatus workflowStatus,
        String title,
        String introductionHtml,
        UUID createdBy,
        UUID reviewRequestedBy,
        UUID approvedBy,
        UUID publishedBy,
        String rejectionComment,
        String contentHash,
        long versionNo
) {
}
