package kr.or.arte.seal.admin.application.role;

import java.util.UUID;

public record PermissionSummary(
        UUID permissionId,
        String permissionCode,
        String permissionName,
        String description
) {
}
