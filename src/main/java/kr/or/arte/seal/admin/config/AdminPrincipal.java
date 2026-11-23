package kr.or.arte.seal.admin.config;

import java.io.Serializable;
import java.util.UUID;

public record AdminPrincipal(
        UUID adminUserId,
        String loginId,
        String displayName,
        UUID organizationUnitId,
        boolean mustChangePassword,
        String securityStamp
) implements Serializable {
    public AdminPrincipal(UUID adminUserId, String loginId, String displayName,
            UUID organizationUnitId, boolean mustChangePassword) {
        this(adminUserId, loginId, displayName, organizationUnitId, mustChangePassword, null);
    }
}
