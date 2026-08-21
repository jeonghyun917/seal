package kr.or.arte.seal.admin.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record AdminAccount(
        UUID adminUserId,
        String loginId,
        String displayName,
        UUID organizationUnitId,
        String passwordHash,
        AdminAccountStatus status,
        int consecutiveFailures,
        Instant lockedUntil,
        boolean mustChangePassword,
        Instant lastLoginAt,
        Set<String> roles,
        Set<String> permissions,
        long versionNo
) {

    public AdminAccount {
        Objects.requireNonNull(adminUserId, "adminUserId");
        Objects.requireNonNull(loginId, "loginId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(organizationUnitId, "organizationUnitId");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(status, "status");
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
        if (consecutiveFailures < 0 || versionNo < 0) {
            throw new IllegalArgumentException("failure count and version must not be negative");
        }
    }

    public AdminAccount withAuthenticationFailure(LoginFailureResult failure) {
        return new AdminAccount(
                adminUserId, loginId, displayName, organizationUnitId, passwordHash, status,
                failure.consecutiveFailures(), failure.lockedUntil().orElse(null), mustChangePassword,
                lastLoginAt, roles, permissions, Math.addExact(versionNo, 1)
        );
    }

    public AdminAccount withAuthenticationSuccess(Instant loggedInAt) {
        return new AdminAccount(
                adminUserId, loginId, displayName, organizationUnitId, passwordHash, status,
                0, null, mustChangePassword, Objects.requireNonNull(loggedInAt, "loggedInAt"),
                roles, permissions, Math.addExact(versionNo, 1)
        );
    }
}
