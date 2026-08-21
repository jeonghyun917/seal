package kr.or.arte.seal.admin.application.user;

import java.util.UUID;

public record AdminUserInsert(
        UUID adminUserId,
        String loginId,
        String displayName,
        UUID organizationUnitId,
        String passwordHash,
        UUID actorId
) {
}
