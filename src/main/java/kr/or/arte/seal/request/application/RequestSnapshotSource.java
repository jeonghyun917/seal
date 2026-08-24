package kr.or.arte.seal.request.application;

import kr.or.arte.seal.template.application.PublishedTemplateView;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Map;

public record RequestSnapshotSource(String institution, String requestCode,
                                    UUID processingActivityVersionId,
                                    UUID retentionPolicyVersionId,
                                    String retentionDisplayText,
                                    UUID templateVersionId,
                                    String templateContentHash,
                                    Instant expiresAt,
                                    String withdrawalNotice,
                                    PublishedTemplateView template,
                                    Map<Integer, List<UUID>> dataItemIdsByItemOrder) {
    public RequestSnapshotSource(String institution, String requestCode,
            UUID processingActivityVersionId, UUID retentionPolicyVersionId,
            String retentionDisplayText, UUID templateVersionId, String templateContentHash,
            Instant expiresAt, String withdrawalNotice, PublishedTemplateView template) {
        this(institution, requestCode, processingActivityVersionId, retentionPolicyVersionId,
                retentionDisplayText, templateVersionId, templateContentHash, expiresAt,
                withdrawalNotice, template, Map.of());
    }
}
