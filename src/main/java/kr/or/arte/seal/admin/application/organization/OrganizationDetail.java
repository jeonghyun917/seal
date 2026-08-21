package kr.or.arte.seal.admin.application.organization;

import java.util.UUID;

public record OrganizationDetail(
        UUID organizationUnitId,
        String organizationCode,
        String organizationName,
        UUID parentId,
        String parentOrganizationName,
        boolean rootUnit,
        boolean active,
        long versionNo
) {
}
