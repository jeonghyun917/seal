package kr.or.arte.seal.admin.application.role;
import java.util.UUID;
public record RoleDetail(UUID roleId, String roleCode, String roleName,
        boolean systemRole, boolean active, long versionNo) { }
