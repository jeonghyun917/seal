package kr.or.arte.seal.admin.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class RoleForm {
    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,49}$")
    private String roleCode;

    @NotBlank
    @Size(max = 100)
    private String roleName;

    private boolean active = true;
    private Set<UUID> permissionIds = new LinkedHashSet<>();
    private long versionNo;

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Set<UUID> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(Set<UUID> permissionIds) {
        this.permissionIds = permissionIds == null ? new LinkedHashSet<>() : permissionIds;
    }
    public long getVersionNo() { return versionNo; }
    public void setVersionNo(long versionNo) { this.versionNo = versionNo; }
}
