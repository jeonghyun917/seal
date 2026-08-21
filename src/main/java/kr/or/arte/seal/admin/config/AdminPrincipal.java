package kr.or.arte.seal.admin.config;

import java.io.Serializable;
import java.util.UUID;

public record AdminPrincipal(
        UUID adminUserId,
        String loginId,
        String displayName,
        UUID organizationUnitId,
        boolean mustChangePassword
) implements Serializable {
}
