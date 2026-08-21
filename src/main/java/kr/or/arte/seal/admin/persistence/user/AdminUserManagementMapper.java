package kr.or.arte.seal.admin.persistence.user;

import kr.or.arte.seal.admin.application.user.AdminUserDetail;
import kr.or.arte.seal.admin.application.user.AdminUserInsert;
import kr.or.arte.seal.admin.application.user.AdminUserSummary;
import kr.or.arte.seal.admin.application.user.AdminUserUpdate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper
public interface AdminUserManagementMapper {
    List<AdminUserSummary> findVisibleUsers(@Param("adminUserId") UUID adminUserId);
    List<AdminUserDetail> findVisibleUser(@Param("actorId") UUID actorId,
            @Param("adminUserId") UUID adminUserId);
    boolean canAccessOrganization(@Param("actorId") UUID actorId,
            @Param("organizationUnitId") UUID organizationUnitId);
    int countActiveRoles(@Param("roleIds") Set<UUID> roleIds);
    boolean hasSystemAdminRole(@Param("adminUserId") UUID adminUserId);
    boolean includesSystemAdminRole(@Param("roleIds") Set<UUID> roleIds);
    int countActiveSystemAdministrators();
    int insert(AdminUserInsert user);
    int deactivateRolesNotIn(@Param("adminUserId") UUID adminUserId,
            @Param("roleIds") Set<UUID> roleIds, @Param("actorId") UUID actorId);
    int upsertRoles(@Param("adminUserId") UUID adminUserId,
            @Param("roleIds") Set<UUID> roleIds, @Param("actorId") UUID actorId);
    int deactivateOrganizationScopesNotIn(@Param("adminUserId") UUID adminUserId,
            @Param("scopeIds") Set<UUID> scopeIds, @Param("actorId") UUID actorId);
    int upsertOrganizationScopes(@Param("adminUserId") UUID adminUserId,
            @Param("scopeIds") Set<UUID> scopeIds, @Param("actorId") UUID actorId);
    int update(AdminUserUpdate user);
    int unlock(@Param("adminUserId") UUID adminUserId, @Param("versionNo") long versionNo,
            @Param("actorId") UUID actorId);
    int resetPassword(@Param("adminUserId") UUID adminUserId,
            @Param("passwordHash") String passwordHash, @Param("versionNo") long versionNo,
            @Param("actorId") UUID actorId);
    List<UUID> findAssignedRoleIds(@Param("adminUserId") UUID adminUserId);
    List<UUID> findAssignedOrganizationScopeIds(@Param("adminUserId") UUID adminUserId);

    default void replaceRoles(UUID adminUserId, Set<UUID> roleIds, UUID actorId) {
        deactivateRolesNotIn(adminUserId, roleIds, actorId);
        if (!roleIds.isEmpty()) upsertRoles(adminUserId, roleIds, actorId);
    }

    default void replaceOrganizationScopes(UUID adminUserId, Set<UUID> scopeIds, UUID actorId) {
        deactivateOrganizationScopesNotIn(adminUserId, scopeIds, actorId);
        if (!scopeIds.isEmpty()) upsertOrganizationScopes(adminUserId, scopeIds, actorId);
    }
}
