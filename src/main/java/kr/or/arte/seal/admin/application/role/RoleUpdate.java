package kr.or.arte.seal.admin.application.role;
import java.util.UUID;
public record RoleUpdate(UUID roleId, String roleName, boolean active, long versionNo) { }
