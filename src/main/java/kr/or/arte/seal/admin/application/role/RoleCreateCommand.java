package kr.or.arte.seal.admin.application.role;
import java.util.Set;
import java.util.UUID;
public record RoleCreateCommand(String roleCode, String roleName,
        Set<UUID> permissionIds, UUID actorId) { }
