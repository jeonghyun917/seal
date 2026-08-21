package kr.or.arte.seal.admin.application;

import java.util.Objects;
import java.util.UUID;

public record BootstrapAdminCommand(
        UUID adminUserId,
        UUID userScopeId,
        String loginId,
        String displayName,
        String passwordHash,
        boolean mustChangePassword
) {

    public BootstrapAdminCommand {
        Objects.requireNonNull(adminUserId, "adminUserId");
        Objects.requireNonNull(userScopeId, "userScopeId");
        Objects.requireNonNull(loginId, "loginId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(passwordHash, "passwordHash");
    }
}
