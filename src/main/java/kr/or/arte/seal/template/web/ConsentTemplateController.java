package kr.or.arte.seal.template.web;

import jakarta.validation.Valid;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityQuery;
import kr.or.arte.seal.template.application.ConsentItemDraftCommand;
import kr.or.arte.seal.template.application.ConsentTemplateDetail;
import kr.or.arte.seal.template.application.ConsentTemplateManagement;
import kr.or.arte.seal.template.application.ConsentTemplateQueryService;
import kr.or.arte.seal.template.application.CreateConsentTemplateCommand;
import kr.or.arte.seal.template.application.UpdateConsentTemplateDraftCommand;
import kr.or.arte.seal.template.application.TemplatePreviewRenderer;
import kr.or.arte.seal.template.domain.ConsentItemType;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import kr.or.arte.seal.template.application.TemplateBusinessConflictException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class ConsentTemplateController {
    private final ConsentTemplateManagement management;
    private final ConsentTemplateQueryService query;
    private final ApprovedProcessingActivityQuery approvedActivityQuery;
    private final TemplatePreviewRenderer previewRenderer;

    public ConsentTemplateController(ConsentTemplateManagement management,
            ConsentTemplateQueryService query,
            ApprovedProcessingActivityQuery approvedActivityQuery,
            TemplatePreviewRenderer previewRenderer) {
        this.management = management;
        this.query = query;
        this.approvedActivityQuery = approvedActivityQuery;
        this.previewRenderer = previewRenderer;
    }

    @GetMapping("/admin/templates")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_VIEW')")
    public String list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("templates", query.search(principal.adminUserId(), keyword, status));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        return "admin/templates/list";
    }

    @GetMapping("/admin/templates/new")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_EDIT')")
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        ConsentTemplateForm form = new ConsentTemplateForm();
        form.getItems().add(new ConsentItemForm());
        model.addAttribute("templateForm", form);
        populateForm(principal, model, false, null, null);
        return "admin/templates/form";
    }

    @PostMapping("/admin/templates")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_EDIT')")
    public String create(@Valid @ModelAttribute("templateForm") ConsentTemplateForm form,
            BindingResult result, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (result.hasErrors()) {
            populateForm(principal, model, false, null, null);
            return "admin/templates/form";
        }
        UUID templateId = management.create(new CreateConsentTemplateCommand(form.getTemplateCode(),
                form.getTemplateName(), form.getProcessingActivityVersionId(), form.getTitle(),
                form.getIntroductionHtml(), commands(form), principal.adminUserId()));
        return "redirect:/admin/templates/" + templateId + "?created";
    }

    @GetMapping("/admin/templates/{templateId}")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_VIEW')")
    public String detail(@PathVariable UUID templateId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("template", query.requireScoped(templateId, principal.adminUserId()));
        return "admin/templates/detail";
    }

    @GetMapping("/admin/templates/{templateId}/versions/{versionId}/edit")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_EDIT')")
    public String editForm(@PathVariable UUID templateId, @PathVariable UUID versionId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        ConsentTemplateDetail detail = query.requireVersionScoped(templateId, versionId,
                principal.adminUserId());
        model.addAttribute("templateForm", toForm(detail));
        populateForm(principal, model, true, templateId, versionId);
        return "admin/templates/form";
    }

    @PostMapping("/admin/templates/{templateId}/versions/{versionId}")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_EDIT')")
    public String update(@PathVariable UUID templateId, @PathVariable UUID versionId,
            @Valid @ModelAttribute("templateForm") ConsentTemplateForm form, BindingResult result,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (result.hasErrors()) {
            populateForm(principal, model, true, templateId, versionId);
            return "admin/templates/form";
        }
        management.updateDraft(new UpdateConsentTemplateDraftCommand(templateId, versionId,
                form.getTemplateName(), form.getTitle(), form.getIntroductionHtml(), commands(form),
                form.getTemplateVersionNo(), form.getContentVersionNo(), principal.adminUserId()));
        return redirect(templateId, "updated");
    }

    @GetMapping("/admin/templates/{templateId}/versions/{versionId}/preview")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_VIEW')")
    public String preview(@PathVariable UUID templateId, @PathVariable UUID versionId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        ConsentTemplateDetail detail = query.requireVersionScoped(templateId, versionId,
                principal.adminUserId());
        var policy = approvedActivityQuery.findReferenceForExistingTemplate(
                principal.adminUserId(), detail.version().processingActivityVersionId());
        model.addAttribute("detail", detail);
        model.addAttribute("preview", previewRenderer.toView(detail, policy));
        return "admin/templates/preview";
    }

    @PostMapping("/admin/templates/{templateId}/versions/{versionId}/request-review")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_REVIEW')")
    public String requestReview(@PathVariable UUID templateId, @PathVariable UUID versionId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        management.requestReview(templateId, versionId, principal.adminUserId());
        return redirect(templateId, "reviewRequested");
    }

    @PostMapping("/admin/templates/{templateId}/versions/{versionId}/approve")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_APPROVE')")
    public String approve(@PathVariable UUID templateId, @PathVariable UUID versionId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        management.approve(templateId, versionId, principal.adminUserId());
        return redirect(templateId, "approved");
    }

    @PostMapping("/admin/templates/{templateId}/versions/{versionId}/reject")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_APPROVE')")
    public String reject(@PathVariable UUID templateId, @PathVariable UUID versionId,
            @RequestParam String comment, @AuthenticationPrincipal AdminPrincipal principal) {
        management.reject(templateId, versionId, principal.adminUserId(), comment);
        return redirect(templateId, "rejected");
    }

    @PostMapping("/admin/templates/{templateId}/versions/{versionId}/publish")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_PUBLISH')")
    public String publish(@PathVariable UUID templateId, @PathVariable UUID versionId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        management.publish(templateId, versionId, principal.adminUserId());
        return redirect(templateId, "published");
    }

    @PostMapping("/admin/templates/{templateId}/versions")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_EDIT')")
    public String createNextVersion(@PathVariable UUID templateId,
            @RequestParam UUID sourceVersionId, @AuthenticationPrincipal AdminPrincipal principal) {
        UUID versionId = management.createNextDraft(templateId, sourceVersionId, principal.adminUserId());
        return "redirect:/admin/templates/" + templateId + "/versions/" + versionId + "/edit?copied";
    }

    @PostMapping("/admin/templates/{templateId}/status")
    @PreAuthorize("hasAuthority('CONSENT_TEMPLATE_PUBLISH')")
    public String updateStatus(@PathVariable UUID templateId, @RequestParam String status,
            @RequestParam long versionNo, @AuthenticationPrincipal AdminPrincipal principal) {
        management.updateStatus(templateId, status, versionNo, principal.adminUserId());
        return redirect(templateId, "statusUpdated");
    }

    @ExceptionHandler(TemplateBusinessConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(TemplateBusinessConflictException exception,
            HttpServletRequest request, @AuthenticationPrincipal AdminPrincipal principal,
            Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("conflictMessage", exception.getMessage());
        UUID templateId = templateIdFrom(request.getRequestURI());
        if (templateId != null) {
            model.addAttribute("template", query.requireScoped(templateId, principal.adminUserId()));
            return "admin/templates/detail";
        }
        return "admin/templates/conflict";
    }

    private void populateForm(AdminPrincipal principal, Model model, boolean editing,
                              UUID templateId, UUID versionId) {
        model.addAttribute("administrator", principal);
        model.addAttribute("activities", approvedActivityQuery.findAllApprovedForTemplate(
                principal.adminUserId()));
        model.addAttribute("itemTypes", ConsentItemType.values());
        model.addAttribute("editing", editing);
        model.addAttribute("templateId", templateId);
        model.addAttribute("versionId", versionId);
    }

    private static List<ConsentItemDraftCommand> commands(ConsentTemplateForm form) {
        return form.getItems().stream().map(item -> new ConsentItemDraftCommand(item.getCode(),
                item.getType(), item.getTitle(), item.isRequired(), item.getPurpose(),
                item.getRetentionText(), item.getRefusalNotice(), item.getThirdPartyRecipient(),
                item.getProcessingDataItemIds())).toList();
    }

    private ConsentTemplateForm toForm(ConsentTemplateDetail detail) {
        ConsentTemplateForm form = new ConsentTemplateForm();
        form.setTemplateCode(detail.templateCode());
        form.setTemplateName(detail.templateName());
        form.setProcessingActivityVersionId(detail.version().processingActivityVersionId());
        form.setTitle(detail.version().title());
        form.setIntroductionHtml(detail.version().introductionHtml());
        List<ConsentItemForm> items = new ArrayList<>();
        detail.version().items().forEach(item -> {
            ConsentItemForm entry = new ConsentItemForm();
            entry.setCode(item.code()); entry.setType(item.type()); entry.setTitle(item.title());
            entry.setRequired(item.required()); entry.setPurpose(item.purpose());
            entry.setRetentionText(item.retentionText()); entry.setRefusalNotice(item.refusalNotice());
            entry.setThirdPartyRecipient(item.thirdPartyRecipient());
            entry.setProcessingDataItemIds(item.processingDataItemIds()); items.add(entry);
        });
        form.setItems(items);
        form.setTemplateVersionNo(detail.templateVersionNo());
        form.setContentVersionNo(detail.version().versionNo());
        return form;
    }

    private static String redirect(UUID templateId, String event) {
        return "redirect:/admin/templates/" + templateId + "?" + event;
    }

    private static UUID templateIdFrom(String uri) {
        String marker = "/admin/templates/";
        int start = uri.indexOf(marker);
        if (start < 0) return null;
        String remainder = uri.substring(start + marker.length());
        String candidate = remainder.contains("/")
                ? remainder.substring(0, remainder.indexOf('/')) : remainder;
        try {
            return UUID.fromString(candidate);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
