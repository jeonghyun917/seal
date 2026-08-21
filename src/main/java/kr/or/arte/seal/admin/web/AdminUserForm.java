package kr.or.arte.seal.admin.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class AdminUserForm {
    @NotBlank @Pattern(regexp = "^[a-z0-9.-]{4,50}$")
    private String loginId;
    @NotBlank @Size(max = 100)
    private String displayName;
    @NotNull
    private UUID organizationUnitId;
    private String accountStatus = "ACTIVE";
    private String temporaryPassword;
    private Set<UUID> roleIds = new LinkedHashSet<>();
    private Set<UUID> organizationScopeIds = new LinkedHashSet<>();
    private long versionNo;

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UUID getOrganizationUnitId() { return organizationUnitId; }
    public void setOrganizationUnitId(UUID organizationUnitId) { this.organizationUnitId = organizationUnitId; }
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public String getTemporaryPassword() { return temporaryPassword; }
    public void setTemporaryPassword(String temporaryPassword) { this.temporaryPassword = temporaryPassword; }
    public Set<UUID> getRoleIds() { return roleIds; }
    public void setRoleIds(Set<UUID> roleIds) { this.roleIds = roleIds == null ? new LinkedHashSet<>() : roleIds; }
    public Set<UUID> getOrganizationScopeIds() { return organizationScopeIds; }
    public void setOrganizationScopeIds(Set<UUID> organizationScopeIds) { this.organizationScopeIds = organizationScopeIds == null ? new LinkedHashSet<>() : organizationScopeIds; }
    public long getVersionNo() { return versionNo; }
    public void setVersionNo(long versionNo) { this.versionNo = versionNo; }
}
