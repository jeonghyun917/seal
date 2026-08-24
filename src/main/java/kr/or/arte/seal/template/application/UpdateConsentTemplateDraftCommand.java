package kr.or.arte.seal.template.application;

import java.util.List;
import java.util.UUID;

public record UpdateConsentTemplateDraftCommand(
        UUID templateId,
        UUID versionId,
        String templateName,
        String title,
        String introductionHtml,
        List<ConsentItemDraftCommand> items,
        long templateVersionNo,
        long contentVersionNo,
        UUID actorId
) {
}
