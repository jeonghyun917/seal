package kr.or.arte.seal.admin.application.role;
import java.util.Set;
import java.util.UUID;
public record RoleUpdateCommand(UUID roleId, String roleName, boolean active,
        Set<UUID> permissionIds, long versionNo, UUID actorId) { }
