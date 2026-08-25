package kr.or.arte.seal.recipient.web;

import jakarta.validation.Valid;
import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.recipient.application.CreateRecipientCommand;
import kr.or.arte.seal.recipient.application.DuplicateRecipientException;
import kr.or.arte.seal.recipient.application.RecipientDetail;
import kr.or.arte.seal.recipient.application.RecipientManagementService;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.recipient.application.UpdateRecipientCommand;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
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

import java.util.UUID;

@Controller
public class RecipientController {
    private final RecipientManagementService management;
    private final RecipientQueryService query;
    private final OrganizationManagement organizations;

    public RecipientController(RecipientManagementService management, RecipientQueryService query,
                               OrganizationManagement organizations) {
        this.management = management;
        this.query = query;
        this.organizations = organizations;
    }

    @GetMapping("/admin/recipients")
    @PreAuthorize("hasAuthority('RECIPIENT_VIEW')")
    public String list(@RequestParam(required = false) String email,
            @RequestParam(required = false) RecipientStatus status,
            @RequestParam(defaultValue = "1") int page, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        var pageResult=query.searchPage(principal.adminUserId(),email,status,page,20); model.addAttribute("recipients",pageResult.items()); model.addAttribute("pageResult",pageResult);
        model.addAttribute("email", email);
        model.addAttribute("selectedStatus", status);
        return "admin/recipients/list";
    }

    @GetMapping("/admin/recipients/new")
    @PreAuthorize("hasAuthority('RECIPIENT_EDIT')")
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("recipientForm", new RecipientForm());
        populate(principal, model, false, null);
        return "admin/recipients/form";
    }

    @PostMapping("/admin/recipients")
    @PreAuthorize("hasAuthority('RECIPIENT_EDIT')")
    public String create(@Valid @ModelAttribute("recipientForm") RecipientForm form,
            BindingResult result, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        requireCreatePersonalData(form, result);
        if (result.hasErrors()) {
            populate(principal, model, false, null);
            return "admin/recipients/form";
        }
        try {
            UUID id = management.create(new CreateRecipientCommand(principal.adminUserId(),
                    form.getOrganizationUnitId(), form.getName(), form.getEmail(), form.getCollectionBasis()));
            return "redirect:/admin/recipients/" + id + "?created";
        } catch (DuplicateRecipientException exception) {
            result.rejectValue("email", "duplicate", exception.getMessage());
            populate(principal, model, false, null);
            return "admin/recipients/form";
        }
    }

    @GetMapping("/admin/recipients/{recipientId}")
    @PreAuthorize("hasAuthority('RECIPIENT_VIEW')")
    public String detail(@PathVariable UUID recipientId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("recipient", query.find(principal.adminUserId(), recipientId));
        return "admin/recipients/detail";
    }

    @GetMapping("/admin/recipients/{recipientId}/edit")
    @PreAuthorize("hasAuthority('RECIPIENT_EDIT')")
    public String editForm(@PathVariable UUID recipientId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        RecipientDetail detail = query.find(principal.adminUserId(), recipientId);
        RecipientForm form = new RecipientForm();
        form.setOrganizationUnitId(detail.organizationUnitId());
        form.setCollectionBasis(detail.collectionBasis());
        form.setVersionNo(detail.versionNo());
        model.addAttribute("recipientForm", form);
        populate(principal, model, true, detail);
        return "admin/recipients/form";
    }

    @PostMapping("/admin/recipients/{recipientId}")
    @PreAuthorize("hasAuthority('RECIPIENT_EDIT')")
    public String update(@PathVariable UUID recipientId,
            @Valid @ModelAttribute("recipientForm") RecipientForm form, BindingResult result,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (result.hasErrors()) {
            populate(principal, model, true, query.find(principal.adminUserId(), recipientId));
            return "admin/recipients/form";
        }
        try {
            management.update(recipientId, new UpdateRecipientCommand(principal.adminUserId(),
                    form.getName(), form.getEmail(), form.getCollectionBasis(), form.getVersionNo()));
            return "redirect:/admin/recipients/" + recipientId + "?updated";
        } catch (DuplicateRecipientException exception) {
            result.rejectValue("email", "duplicate", exception.getMessage());
            populate(principal, model, true, query.find(principal.adminUserId(), recipientId));
            return "admin/recipients/form";
        }
    }

    @PostMapping("/admin/recipients/{recipientId}/status")
    @PreAuthorize("hasAuthority('RECIPIENT_EDIT')")
    public String status(@PathVariable UUID recipientId, @RequestParam RecipientStatus status,
            @RequestParam long versionNo, @AuthenticationPrincipal AdminPrincipal principal) {
        management.changeStatus(recipientId, principal.adminUserId(), status, versionNo);
        return "redirect:/admin/recipients/" + recipientId + "?statusUpdated";
    }

    private void populate(AdminPrincipal principal, Model model, boolean editing, RecipientDetail detail) {
        model.addAttribute("administrator", principal);
        model.addAttribute("organizations", organizations.findVisibleOrganizations(principal.adminUserId()));
        model.addAttribute("editing", editing);
        model.addAttribute("recipient", detail);
    }

    private static void requireCreatePersonalData(RecipientForm form, BindingResult result) {
        if (form.getName() == null || form.getName().isBlank()) result.rejectValue("name", "required", "이름을 입력해 주세요.");
        if (form.getEmail() == null || form.getEmail().isBlank()) result.rejectValue("email", "required", "이메일을 입력해 주세요.");
    }
}
