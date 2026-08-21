package kr.or.arte.seal.admin.application.user;

import java.time.Instant;
import java.util.UUID;

public record AdminUserDetail(
        UUID adminUserId,
        String loginId,
        String displayName,
        UUID organizationUnitId,
        String organizationName,
        String accountStatus,
        int consecutiveFailures,
        Instant lockedUntil,
        boolean mustChangePassword,
        long versionNo
) {
}
