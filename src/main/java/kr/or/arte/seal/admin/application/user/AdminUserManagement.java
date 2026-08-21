package kr.or.arte.seal.admin.application.user;

import java.util.List;
import java.util.UUID;

public interface AdminUserManagement extends AdminUserQuery {
    AdminUserDetail findVisibleUser(UUID actorId, UUID adminUserId);
    UUID create(AdminUserCreateCommand command);
    void update(AdminUserUpdateCommand command);
    void unlock(UUID adminUserId, UUID actorId);
    void resetPassword(UUID adminUserId, String temporaryPassword, UUID actorId);
    List<UUID> findAssignedRoleIds(UUID actorId, UUID adminUserId);
    List<UUID> findAssignedOrganizationScopeIds(UUID actorId, UUID adminUserId);
}
