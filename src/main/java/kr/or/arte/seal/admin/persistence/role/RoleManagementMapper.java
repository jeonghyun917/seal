package kr.or.arte.seal.admin.persistence.role;

import kr.or.arte.seal.admin.application.role.RoleDetail;
import kr.or.arte.seal.admin.application.role.RoleInsert;
import kr.or.arte.seal.admin.application.role.PermissionSummary;
import kr.or.arte.seal.admin.application.role.RoleUpdate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper
public interface RoleManagementMapper {
    List<RoleDetail> findRole(@Param("roleId") UUID roleId);
    List<RoleDetail> findRoleForUpdate(@Param("roleId") UUID roleId);
    boolean isSystemAdministrator(@Param("adminUserId") UUID adminUserId);
    List<PermissionSummary> findPermissions();
    Set<UUID> findAssignedPermissionIds(@Param("roleId") UUID roleId);
    Set<UUID> findRequiredSystemAdminPermissionIds();
    int countActiveAssignedUsers(@Param("roleId") UUID roleId);
    int countPermissions(@Param("permissionIds") Set<UUID> permissionIds);
    int insert(RoleInsert role);
    int update(RoleUpdate role);
    int deactivatePermissionsNotIn(@Param("roleId") UUID roleId,
            @Param("permissionIds") Set<UUID> permissionIds, @Param("actorId") UUID actorId);
    int upsertPermissions(@Param("roleId") UUID roleId,
            @Param("permissionIds") Set<UUID> permissionIds);

    default void replacePermissions(UUID roleId, Set<UUID> permissionIds, UUID actorId) {
        deactivatePermissionsNotIn(roleId, permissionIds, actorId);
        upsertPermissions(roleId, permissionIds);
    }
}
