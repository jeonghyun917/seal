package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.domain.TemplateWorkflowStatus;

import java.util.UUID;

public record ConsentTemplateVersionSummary(
        UUID consentTemplateVersionId,
        int versionNumber,
        TemplateWorkflowStatus workflowStatus,
        String title,
        String createdByName,
        String approvedByName,
        String publishedByName
) {
}
