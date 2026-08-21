package kr.or.arte.seal.policy.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityQuery;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityCreateCommand;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityManagementService;
import kr.or.arte.seal.policy.domain.activity.LegalBasisType;
import kr.or.arte.seal.policy.domain.activity.ProcessingDataItem;
import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class ProcessingActivityController {
    private final ProcessingActivityQuery query;
    private final ProcessingActivityManagementService management;
    private final OrganizationManagement organizations;
    public ProcessingActivityController(ProcessingActivityQuery query,
            ProcessingActivityManagementService management, OrganizationManagement organizations) {
        this.query = query; this.management = management; this.organizations = organizations;
    }
    @GetMapping("/admin/processing-activities") @PreAuthorize("hasAuthority('PROCESSING_ACTIVITY_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("activities", query.findVisibleActivities(principal.adminUserId()));
        return "admin/processing-activities/list";
    }
    @GetMapping("/admin/processing-activities/new") @PreAuthorize("hasAuthority('PROCESSING_ACTIVITY_EDIT')")
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("activityForm", new ProcessingActivityForm()); populate(principal, model);
        return "admin/processing-activities/form";
    }
    @PostMapping("/admin/processing-activities") @PreAuthorize("hasAuthority('PROCESSING_ACTIVITY_EDIT')")
    public String create(@Valid @ModelAttribute("activityForm") ProcessingActivityForm form,
            BindingResult result, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        validateOptionalItem(form.getItemName2(), form.getNecessityReason2(), "itemName2", "necessityReason2", result);
        validateOptionalItem(form.getItemName3(), form.getNecessityReason3(), "itemName3", "necessityReason3", result);
        if (result.hasErrors()) { populate(principal, model); return "admin/processing-activities/form"; }
        UUID id = management.create(new ProcessingActivityCreateCommand(form.getActivityCode(), form.getActivityName(),
                form.getOwnerOrganizationUnitId(), form.getProcessingPurpose(), form.getLegalBasisType(),
                form.getLegalBasisDetail(), form.getRetentionPolicyVersionId(), items(form), principal.adminUserId()));
        return "redirect:/admin/processing-activities/" + id + "?created";
    }
    @GetMapping("/admin/processing-activities/{activityId}") @PreAuthorize("hasAuthority('PROCESSING_ACTIVITY_VIEW')")
    public String detail(@PathVariable UUID activityId, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("activity", query.findVisibleActivity(principal.adminUserId(), activityId));
        model.addAttribute("versions", query.findVisibleVersions(principal.adminUserId(), activityId));
        model.addAttribute("dataItems", query.findVisibleDataItems(principal.adminUserId(), activityId));
        return "admin/processing-activities/detail";
    }
    @PostMapping("/admin/processing-activities/{activityId}/versions/{versionId}/request-review") @PreAuthorize("hasAuthority('PROCESSING_ACTIVITY_REVIEW')")
    public String requestReview(@PathVariable UUID activityId, @PathVariable UUID versionId, @AuthenticationPrincipal AdminPrincipal principal) { management.requestReview(activityId, versionId, principal.adminUserId()); return redirect(activityId, "reviewRequested"); }
    @PostMapping("/admin/processing-activities/{activityId}/versions/{versionId}/approve") @PreAuthorize("hasAuthority('PROCESSING_ACTIVITY_APPROVE')")
    public String approve(@PathVariable UUID activityId, @PathVariable UUID versionId, @AuthenticationPrincipal AdminPrincipal principal) { management.approve(activityId, versionId, principal.adminUserId()); return redirect(activityId, "approved"); }
    @PostMapping("/admin/processing-activities/{activityId}/versions/{versionId}/reject") @PreAuthorize("hasAuthority('PROCESSING_ACTIVITY_APPROVE')")
    public String reject(@PathVariable UUID activityId, @PathVariable UUID versionId, @RequestParam String comment, @AuthenticationPrincipal AdminPrincipal principal) { management.reject(activityId, versionId, principal.adminUserId(), comment); return redirect(activityId, "rejected"); }
    private void populate(AdminPrincipal principal, Model model) { model.addAttribute("administrator", principal); model.addAttribute("organizations", organizations.findVisibleOrganizations(principal.adminUserId())); model.addAttribute("retentionPolicies", query.findApprovedRetentionPolicies(principal.adminUserId())); model.addAttribute("legalBasisTypes", LegalBasisType.values()); }
    private static List<ProcessingDataItem> items(ProcessingActivityForm f) { List<ProcessingDataItem> result = new ArrayList<>(); result.add(new ProcessingDataItem(f.getItemName1(), f.getNecessityReason1(), f.isRequired1())); add(result,f.getItemName2(),f.getNecessityReason2(),f.isRequired2()); add(result,f.getItemName3(),f.getNecessityReason3(),f.isRequired3()); return result; }
    private static void add(List<ProcessingDataItem> items,String name,String reason,boolean required){if(name!=null&&!name.isBlank())items.add(new ProcessingDataItem(name,reason,required));}
    private static void validateOptionalItem(String name,String reason,String nameField,String reasonField,BindingResult result){boolean hasName=name!=null&&!name.isBlank(), hasReason=reason!=null&&!reason.isBlank(); if(hasName&&!hasReason)result.rejectValue(reasonField,"required","필요성 설명을 입력해 주세요."); if(!hasName&&hasReason)result.rejectValue(nameField,"required","항목명을 입력해 주세요.");}
    private static String redirect(UUID id,String event){return "redirect:/admin/processing-activities/"+id+"?"+event;}
}
