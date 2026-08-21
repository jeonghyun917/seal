package kr.or.arte.seal.admin.application.user;

import java.util.UUID;

public record AdminUserUpdate(
        UUID adminUserId,
        String displayName,
        UUID organizationUnitId,
        String accountStatus,
        long versionNo,
        UUID actorId
) {
}
