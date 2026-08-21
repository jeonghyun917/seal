package kr.or.arte.seal.admin.application.user;

import java.util.Set;
import java.util.UUID;

public record AdminUserUpdateCommand(
        UUID adminUserId,
        String displayName,
        UUID organizationUnitId,
        String accountStatus,
        Set<UUID> roleIds,
        Set<UUID> organizationScopeIds,
        long versionNo,
        UUID actorId
) {
}
