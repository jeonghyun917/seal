package kr.or.arte.seal.admin.persistence;

import java.time.Instant;
import java.util.UUID;

public record AdminAccountRow(
        UUID adminUserId,
        String loginId,
        String displayName,
        UUID organizationUnitId,
        String passwordHash,
        String accountStatus,
        int consecutiveFailures,
        Instant lockedUntil,
        boolean mustChangePassword,
        Instant lastLoginAt,
        long versionNo,
        String roleCode,
        String permissionCode
) {
}
