package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import kr.or.arte.seal.admin.application.role.RoleQuery;
import kr.or.arte.seal.admin.application.user.AdminUserCreateCommand;
import kr.or.arte.seal.admin.application.user.AdminUserDetail;
import kr.or.arte.seal.admin.application.user.AdminUserManagement;
import kr.or.arte.seal.admin.application.user.AdminUserUpdateCommand;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashSet;
import java.util.UUID;

@Controller
public class AdminUserController {
    private final AdminUserManagement management;
    private final OrganizationManagement organizationManagement;
    private final RoleQuery roleQuery;

    public AdminUserController(AdminUserManagement management,
            OrganizationManagement organizationManagement, RoleQuery roleQuery) {
        this.management = management;
        this.organizationManagement = organizationManagement;
        this.roleQuery = roleQuery;
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("users", management.findVisibleUsers(principal.adminUserId()));
        return "admin/users/list";
    }

    @GetMapping("/admin/users/new")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("adminUserForm", new AdminUserForm());
        populateForm(principal, model, false, null);
        return "admin/users/form";
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String create(@Valid @ModelAttribute("adminUserForm") AdminUserForm form,
            BindingResult bindingResult, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (form.getTemporaryPassword() == null || form.getTemporaryPassword().isBlank()) {
            bindingResult.rejectValue("temporaryPassword", "required", "임시 비밀번호를 입력해 주세요.");
        }
        validateAssignments(form, bindingResult);
        if (bindingResult.hasErrors()) {
            form.setTemporaryPassword(null);
            populateForm(principal, model, false, null);
            return "admin/users/form";
        }
        UUID userId = management.create(new AdminUserCreateCommand(form.getLoginId(),
                form.getDisplayName(), form.getOrganizationUnitId(), form.getTemporaryPassword(),
                form.getRoleIds(), form.getOrganizationScopeIds(), principal.adminUserId()));
        form.setTemporaryPassword(null);
        return "redirect:/admin/users/" + userId + "?created";
    }

    @GetMapping("/admin/users/{userId}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public String detail(@PathVariable UUID userId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("user", management.findVisibleUser(principal.adminUserId(), userId));
        model.addAttribute("assignedRoleIds", management.findAssignedRoleIds(principal.adminUserId(), userId));
        model.addAttribute("assignedScopeIds", management.findAssignedOrganizationScopeIds(principal.adminUserId(), userId));
        model.addAttribute("roles", roleQuery.findRoles());
        model.addAttribute("organizations", organizationManagement.findVisibleOrganizations(principal.adminUserId()));
        return "admin/users/detail";
    }

    @GetMapping("/admin/users/{userId}/edit")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String editForm(@PathVariable UUID userId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        AdminUserDetail user = management.findVisibleUser(principal.adminUserId(), userId);
        AdminUserForm form = new AdminUserForm();
        form.setLoginId(user.loginId());
        form.setDisplayName(user.displayName());
        form.setOrganizationUnitId(user.organizationUnitId());
        form.setAccountStatus(user.accountStatus());
        form.setRoleIds(new LinkedHashSet<>(management.findAssignedRoleIds(principal.adminUserId(), userId)));
        form.setOrganizationScopeIds(new LinkedHashSet<>(management.findAssignedOrganizationScopeIds(principal.adminUserId(), userId)));
        form.setVersionNo(user.versionNo());
        model.addAttribute("adminUserForm", form);
        populateForm(principal, model, true, user);
        return "admin/users/form";
    }

    @PostMapping("/admin/users/{userId}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String update(@PathVariable UUID userId,
            @Valid @ModelAttribute("adminUserForm") AdminUserForm form,
            BindingResult bindingResult, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        AdminUserDetail user = management.findVisibleUser(principal.adminUserId(), userId);
        validateAssignments(form, bindingResult);
        form.setTemporaryPassword(null);
        if (bindingResult.hasErrors()) {
            populateForm(principal, model, true, user);
            return "admin/users/form";
        }
        management.update(new AdminUserUpdateCommand(userId, form.getDisplayName(),
                form.getOrganizationUnitId(), form.getAccountStatus(), form.getRoleIds(),
                form.getOrganizationScopeIds(), form.getVersionNo(), principal.adminUserId()));
        return "redirect:/admin/users/" + userId + "?updated";
    }

    @PostMapping("/admin/users/{userId}/unlock")
    @PreAuthorize("hasAuthority('USER_UNLOCK')")
    public String unlock(@PathVariable UUID userId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        management.unlock(userId, principal.adminUserId());
        return "redirect:/admin/users/" + userId + "?unlocked";
    }

    @PostMapping("/admin/users/{userId}/reset-password")
    @PreAuthorize("hasAuthority('USER_RESET_PASSWORD')")
    public String resetPassword(@PathVariable UUID userId,
            @RequestParam String temporaryPassword,
            @AuthenticationPrincipal AdminPrincipal principal) {
        management.resetPassword(userId, temporaryPassword, principal.adminUserId());
        return "redirect:/admin/users/" + userId + "?passwordReset";
    }

    private void populateForm(AdminPrincipal principal, Model model,
            boolean editing, AdminUserDetail user) {
        model.addAttribute("administrator", principal);
        model.addAttribute("organizations", organizationManagement.findVisibleOrganizations(principal.adminUserId()));
        model.addAttribute("roles", roleQuery.findRoles());
        model.addAttribute("editing", editing);
        model.addAttribute("user", user);
    }

    private static void validateAssignments(AdminUserForm form, BindingResult bindingResult) {
        if (form.getRoleIds().isEmpty()) bindingResult.rejectValue("roleIds", "required", "역할을 하나 이상 선택해 주세요.");
        if (form.getOrganizationScopeIds().isEmpty()) bindingResult.rejectValue("organizationScopeIds", "required", "조직 범위를 하나 이상 선택해 주세요.");
    }
}
