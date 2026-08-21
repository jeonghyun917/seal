package kr.or.arte.seal.admin.application.role;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RoleManagement {
    RoleDetail findRole(UUID roleId);
    List<PermissionSummary> findPermissions();
    Set<UUID> findAssignedPermissionIds(UUID roleId);
    UUID create(RoleCreateCommand command);
    void update(RoleUpdateCommand command);
}
