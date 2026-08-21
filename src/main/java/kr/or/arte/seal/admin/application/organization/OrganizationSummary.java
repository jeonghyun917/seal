package kr.or.arte.seal.admin.application.organization;

import java.util.UUID;

public record OrganizationSummary(
        UUID organizationUnitId,
        String organizationCode,
        String organizationName,
        String parentOrganizationName,
        boolean rootUnit,
        boolean active,
        long versionNo
) {
}
