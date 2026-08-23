package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.domain.ConsentTemplateVersion;

import java.util.UUID;

public record ConsentTemplateDetail(
        UUID consentTemplateId,
        String templateCode,
        String templateName,
        UUID processingActivityId,
        String processingActivityName,
        String templateStatus,
        UUID currentPublishedVersionId,
        long templateVersionNo,
        ConsentTemplateVersion version
) {
}
