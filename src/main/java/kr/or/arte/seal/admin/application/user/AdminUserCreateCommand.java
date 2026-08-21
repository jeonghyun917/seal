package kr.or.arte.seal.admin.application.user;

import java.util.Set;
import java.util.UUID;

public record AdminUserCreateCommand(
        String loginId,
        String displayName,
        UUID organizationUnitId,
        String temporaryPassword,
        Set<UUID> roleIds,
        Set<UUID> organizationScopeIds,
        UUID actorId
) {
}
