package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import kr.or.arte.seal.admin.application.organization.OrganizationCreateCommand;
import kr.or.arte.seal.admin.application.organization.OrganizationDetail;
import kr.or.arte.seal.admin.application.organization.OrganizationUpdateCommand;
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

import java.util.UUID;

@Controller
public class OrganizationController {

    private final OrganizationManagement organizationManagement;

    public OrganizationController(OrganizationManagement organizationManagement) {
        this.organizationManagement = organizationManagement;
    }

    @GetMapping("/admin/organizations")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("organizations",
                organizationManagement.findVisibleOrganizations(principal.adminUserId()));
        return "admin/organizations/list";
    }

    @GetMapping("/admin/organizations/new")
    @PreAuthorize("hasAuthority('ORGANIZATION_MANAGE')")
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("organizationForm", new OrganizationForm());
        populateForm(principal, model, false, null);
        return "admin/organizations/form";
    }

    @PostMapping("/admin/organizations")
    @PreAuthorize("hasAuthority('ORGANIZATION_MANAGE')")
    public String create(@Valid @ModelAttribute("organizationForm") OrganizationForm form,
            BindingResult bindingResult, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (form.getParentId() == null) {
            bindingResult.rejectValue("parentId", "required", "상위 조직을 선택해 주세요.");
        }
        if (bindingResult.hasErrors()) {
            populateForm(principal, model, false, null);
            return "admin/organizations/form";
        }
        UUID organizationId = organizationManagement.create(new OrganizationCreateCommand(
                form.getOrganizationCode(), form.getOrganizationName(), form.getParentId(),
                principal.adminUserId()));
        return "redirect:/admin/organizations/" + organizationId + "?created";
    }

    @GetMapping("/admin/organizations/{organizationId}")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
    public String detail(@PathVariable UUID organizationId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("organization",
                organizationManagement.findVisibleOrganization(principal.adminUserId(), organizationId));
        return "admin/organizations/detail";
    }

    @GetMapping("/admin/organizations/{organizationId}/edit")
    @PreAuthorize("hasAuthority('ORGANIZATION_MANAGE')")
    public String editForm(@PathVariable UUID organizationId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        OrganizationDetail detail = organizationManagement.findVisibleOrganization(
                principal.adminUserId(), organizationId);
        OrganizationForm form = new OrganizationForm();
        form.setOrganizationCode(detail.organizationCode());
        form.setOrganizationName(detail.organizationName());
        form.setParentId(detail.parentId());
        form.setActive(detail.active());
        form.setVersionNo(detail.versionNo());
        model.addAttribute("organizationForm", form);
        populateForm(principal, model, true, detail);
        return "admin/organizations/form";
    }

    @PostMapping("/admin/organizations/{organizationId}")
    @PreAuthorize("hasAuthority('ORGANIZATION_MANAGE')")
    public String update(@PathVariable UUID organizationId,
            @Valid @ModelAttribute("organizationForm") OrganizationForm form,
            BindingResult bindingResult, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        OrganizationDetail detail = organizationManagement.findVisibleOrganization(
                principal.adminUserId(), organizationId);
        if (!detail.rootUnit() && form.getParentId() == null) {
            bindingResult.rejectValue("parentId", "required", "상위 조직을 선택해 주세요.");
        }
        if (bindingResult.hasErrors()) {
            populateForm(principal, model, true, detail);
            return "admin/organizations/form";
        }
        organizationManagement.update(new OrganizationUpdateCommand(organizationId,
                form.getOrganizationName(), form.getParentId(), form.isActive(),
                form.getVersionNo(), principal.adminUserId()));
        return "redirect:/admin/organizations/" + organizationId + "?updated";
    }

    private void populateForm(AdminPrincipal principal, Model model,
            boolean editing, OrganizationDetail organization) {
        model.addAttribute("administrator", principal);
        model.addAttribute("parentOrganizations",
                organizationManagement.findVisibleOrganizations(principal.adminUserId()));
        model.addAttribute("editing", editing);
        model.addAttribute("organization", organization);
    }
}
