package kr.or.arte.seal.admin.application.organization;

import java.util.UUID;

public record OrganizationUpdateCommand(
        UUID organizationUnitId,
        String organizationName,
        UUID parentId,
        boolean active,
        long versionNo,
        UUID actorId
) {
}
