package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.application.role.RoleQuery;
import kr.or.arte.seal.admin.application.role.RoleCreateCommand;
import kr.or.arte.seal.admin.application.role.RoleDetail;
import kr.or.arte.seal.admin.application.role.RoleManagement;
import kr.or.arte.seal.admin.application.role.RoleUpdateCommand;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;

import java.util.LinkedHashSet;
import java.util.UUID;

@Controller
public class RoleController {
    private final RoleQuery query;
    private final RoleManagement management;

    public RoleController(RoleQuery query, RoleManagement management) {
        this.query = query;
        this.management = management;
    }

    @GetMapping("/admin/roles")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("roles", query.findRoles());
        return "admin/roles/list";
    }

    @GetMapping("/admin/roles/new")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("roleForm", new RoleForm());
        populateForm(principal, model, false, null);
        return "admin/roles/form";
    }

    @PostMapping("/admin/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public String create(@Valid @ModelAttribute("roleForm") RoleForm form,
            BindingResult bindingResult, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        validatePermissions(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateForm(principal, model, false, null);
            return "admin/roles/form";
        }
        UUID roleId = management.create(new RoleCreateCommand(form.getRoleCode(), form.getRoleName(),
                form.getPermissionIds(), principal.adminUserId()));
        return "redirect:/admin/roles/" + roleId + "?created";
    }

    @GetMapping("/admin/roles/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public String detail(@PathVariable UUID roleId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("role", management.findRole(roleId));
        model.addAttribute("permissions", management.findPermissions());
        model.addAttribute("assignedPermissionIds", management.findAssignedPermissionIds(roleId));
        return "admin/roles/detail";
    }

    @GetMapping("/admin/roles/{roleId}/edit")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public String editForm(@PathVariable UUID roleId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        RoleDetail role = management.findRole(roleId);
        RoleForm form = new RoleForm();
        form.setRoleCode(role.roleCode());
        form.setRoleName(role.roleName());
        form.setActive(role.active());
        form.setVersionNo(role.versionNo());
        form.setPermissionIds(new LinkedHashSet<>(management.findAssignedPermissionIds(roleId)));
        model.addAttribute("roleForm", form);
        populateForm(principal, model, true, role);
        return "admin/roles/form";
    }

    @PostMapping("/admin/roles/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public String update(@PathVariable UUID roleId,
            @Valid @ModelAttribute("roleForm") RoleForm form, BindingResult bindingResult,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        RoleDetail role = management.findRole(roleId);
        validatePermissions(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateForm(principal, model, true, role);
            return "admin/roles/form";
        }
        management.update(new RoleUpdateCommand(roleId, form.getRoleName(), form.isActive(),
                form.getPermissionIds(), form.getVersionNo(), principal.adminUserId()));
        return "redirect:/admin/roles/" + roleId + "?updated";
    }

    private void populateForm(AdminPrincipal principal, Model model,
            boolean editing, RoleDetail role) {
        model.addAttribute("administrator", principal);
        model.addAttribute("permissions", management.findPermissions());
        model.addAttribute("editing", editing);
        model.addAttribute("role", role);
    }

    private static void validatePermissions(RoleForm form, BindingResult bindingResult) {
        if (form.getPermissionIds().isEmpty()) {
            bindingResult.rejectValue("permissionIds", "required", "권한을 하나 이상 선택해 주세요.");
        }
    }
}
