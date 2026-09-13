package kr.or.arte.seal.template.application;

import java.time.Instant;
import java.util.UUID;

public record DeletedConsentTemplateSummary(
        UUID consentTemplateId,
        String templateCode,
        String templateName,
        String processingActivityName,
        String templateStatus,
        Instant deletedAt,
        String deletedByName,
        String deleteReason
) {
}