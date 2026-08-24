package kr.or.arte.seal.request.application;

import kr.or.arte.seal.template.application.PublishedTemplateView;

import java.time.Instant;
import java.util.UUID;

public record RequestSnapshotSource(String institution, String requestCode,
                                    UUID processingActivityVersionId,
                                    UUID retentionPolicyVersionId,
                                    String retentionDisplayText,
                                    UUID templateVersionId,
                                    String templateContentHash,
                                    Instant expiresAt,
                                    String withdrawalNotice,
                                    PublishedTemplateView template) {
}
