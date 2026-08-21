package kr.or.arte.seal.admin.application.organization;

import java.util.UUID;

public record OrganizationUpdate(
        UUID organizationUnitId,
        String organizationName,
        UUID parentId,
        boolean active,
        long versionNo
) {
}
