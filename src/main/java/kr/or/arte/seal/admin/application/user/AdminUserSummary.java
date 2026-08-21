package kr.or.arte.seal.admin.application.user;

import java.time.Instant;
import java.util.UUID;

public record AdminUserSummary(
        UUID adminUserId,
        String loginId,
        String displayName,
        String organizationName,
        String accountStatus,
        int consecutiveFailures,
        Instant lockedUntil,
        Instant lastLoginAt
) {
}
