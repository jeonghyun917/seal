package kr.or.arte.seal.admin.application.organization;

import java.util.UUID;

public record OrganizationInsert(
        UUID organizationUnitId,
        String organizationCode,
        String organizationName,
        UUID parentId
) {
}
