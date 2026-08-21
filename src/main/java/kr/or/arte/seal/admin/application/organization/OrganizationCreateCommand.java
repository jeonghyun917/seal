package kr.or.arte.seal.admin.application.organization;

import java.util.UUID;

public record OrganizationCreateCommand(
        String organizationCode,
        String organizationName,
        UUID parentId,
        UUID actorId
) {
}
