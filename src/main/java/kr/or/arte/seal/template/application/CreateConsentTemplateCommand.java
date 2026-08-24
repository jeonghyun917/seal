package kr.or.arte.seal.template.application;

import java.util.List;
import java.util.UUID;

public record CreateConsentTemplateCommand(
        String templateCode,
        String templateName,
        UUID processingActivityVersionId,
        String title,
        String introductionHtml,
        List<ConsentItemDraftCommand> items,
        UUID actorId
) {
}
