package kr.or.arte.seal.retention.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActiveLegalHoldSummary(
        UUID id,
        UUID consentRequestId,
        String reasonCode,
        LocalDate expiresOn,
        UUID heldBy,
        Instant heldAt) {
}
