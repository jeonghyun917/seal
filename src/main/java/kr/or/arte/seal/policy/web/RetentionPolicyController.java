package kr.or.arte.seal.policy.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyCreateCommand;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyManagementService;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyQuery;
import kr.or.arte.seal.policy.domain.retention.DestructionMethod;
import kr.or.arte.seal.policy.domain.retention.RetentionPeriodUnit;
import kr.or.arte.seal.policy.domain.retention.RetentionStartEvent;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class RetentionPolicyController {
    private final RetentionPolicyQuery query;
    private final RetentionPolicyManagementService managementService;
    private final OrganizationManagement organizationManagement;
    public RetentionPolicyController(RetentionPolicyQuery query,
            RetentionPolicyManagementService managementService,
            OrganizationManagement organizationManagement) {
        this.query = query;
        this.managementService = managementService;
        this.organizationManagement = organizationManagement;
    }
    @GetMapping("/admin/retention-policies") @PreAuthorize("hasAuthority('RETENTION_POLICY_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("policies", query.findVisiblePolicies(principal.adminUserId()));
        return "admin/retention-policies/list";
    }

    @GetMapping("/admin/retention-policies/new")
    @PreAuthorize("hasAuthority('RETENTION_POLICY_EDIT')")
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("policyForm", new RetentionPolicyForm());
        populateForm(principal, model);
        return "admin/retention-policies/form";
    }

    @PostMapping("/admin/retention-policies")
    @PreAuthorize("hasAuthority('RETENTION_POLICY_EDIT')")
    public String create(@Valid @ModelAttribute("policyForm") RetentionPolicyForm form,
            BindingResult bindingResult, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (bindingResult.hasErrors()) {
            populateForm(principal, model);
            return "admin/retention-policies/form";
        }
        UUID policyId = managementService.create(new RetentionPolicyCreateCommand(
                form.getPolicyCode(), form.getPolicyName(), form.getOwnerOrganizationUnitId(),
                form.getStartEvent(), form.getPeriodValue(), form.getPeriodUnit(),
                form.getGracePeriodDays(), form.getDestructionMethod(), principal.adminUserId()));
        return "redirect:/admin/retention-policies/" + policyId + "?created";
    }

    @GetMapping("/admin/retention-policies/{policyId}")
    @PreAuthorize("hasAuthority('RETENTION_POLICY_VIEW')")
    public String detail(@PathVariable UUID policyId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("policy", query.findVisiblePolicy(principal.adminUserId(), policyId));
        model.addAttribute("versions", query.findVisibleVersions(principal.adminUserId(), policyId));
        return "admin/retention-policies/detail";
    }

    @PostMapping("/admin/retention-policies/{policyId}/versions/{versionId}/request-review")
    @PreAuthorize("hasAuthority('RETENTION_POLICY_REVIEW')")
    public String requestReview(@PathVariable UUID policyId, @PathVariable UUID versionId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        managementService.requestReview(policyId, versionId, principal.adminUserId());
        return "redirect:/admin/retention-policies/" + policyId + "?reviewRequested";
    }

    @PostMapping("/admin/retention-policies/{policyId}/versions/{versionId}/approve")
    @PreAuthorize("hasAuthority('RETENTION_POLICY_APPROVE')")
    public String approve(@PathVariable UUID policyId, @PathVariable UUID versionId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        managementService.approve(policyId, versionId, principal.adminUserId());
        return "redirect:/admin/retention-policies/" + policyId + "?approved";
    }

    @PostMapping("/admin/retention-policies/{policyId}/versions/{versionId}/reject")
    @PreAuthorize("hasAuthority('RETENTION_POLICY_APPROVE')")
    public String reject(@PathVariable UUID policyId, @PathVariable UUID versionId,
            @RequestParam String comment, @AuthenticationPrincipal AdminPrincipal principal) {
        managementService.reject(policyId, versionId, principal.adminUserId(), comment);
        return "redirect:/admin/retention-policies/" + policyId + "?rejected";
    }

    private void populateForm(AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("organizations",
                organizationManagement.findVisibleOrganizations(principal.adminUserId()));
        model.addAttribute("startEvents", RetentionStartEvent.values());
        model.addAttribute("periodUnits", RetentionPeriodUnit.values());
        model.addAttribute("destructionMethods", DestructionMethod.values());
    }
}
