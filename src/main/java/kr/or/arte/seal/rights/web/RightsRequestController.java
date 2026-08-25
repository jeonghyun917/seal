package kr.or.arte.seal.rights.web;

import jakarta.validation.Valid;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import kr.or.arte.seal.request.application.ConsentRequestQueryService;
import kr.or.arte.seal.rights.application.RightsRequestService;
import kr.or.arte.seal.rights.domain.RightsRequestStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping({"/admin/rights", "/admin/rights-requests"})
public class RightsRequestController {
    private final RightsRequestService service;
    private final RecipientQueryService recipients;
    private final ConsentRequestQueryService consentRequests;

    public RightsRequestController(RightsRequestService service, RecipientQueryService recipients,
                                   ConsentRequestQueryService consentRequests) {
        this.service = service;
        this.recipients = recipients;
        this.consentRequests = consentRequests;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RIGHTS_VIEW')")
    public String list(@RequestParam(required = false) RightsRequestStatus status,
            @RequestParam(defaultValue = "1") int page, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        var pageResult=service.searchPage(principal.adminUserId(),status,page,20); model.addAttribute("items",pageResult.items()); model.addAttribute("pageResult",pageResult);
        model.addAttribute("selectedStatus", status);
        return "admin/rights/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('RIGHTS_EDIT')")
    public String form(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        populateOptions(principal, model);
        model.addAttribute("rightsRequestForm", new RightsRequestForm());
        return "admin/rights/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RIGHTS_EDIT')")
    public String create(@Valid @ModelAttribute("rightsRequestForm") RightsRequestForm form,
            BindingResult errors, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (errors.hasErrors()) {
            populateOptions(principal, model);
            return "admin/rights/form";
        }
        var recipient = recipients.find(principal.adminUserId(), form.getRecipientId());
        if (form.getConsentRequestId() != null) {
            var consentRequest = consentRequests.require(principal.adminUserId(), form.getConsentRequestId());
            if (!recipient.recipientId().equals(consentRequest.recipientId())
                    || !recipient.organizationUnitId().equals(consentRequest.organizationUnitId())) {
                throw new IllegalArgumentException("선택한 동의요청이 대상자와 일치하지 않습니다.");
            }
        }
        UUID id = service.create(recipient.organizationUnitId(), form.getRecipientId(), form.getConsentRequestId(),
                form.getType(), form.getReceivedDate(), form.getDueDate(), form.getDetail(),
                principal.adminUserId());
        return "redirect:/admin/rights/" + id;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RIGHTS_VIEW')")
    public String detail(@PathVariable UUID id, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("item", service.require(principal.adminUserId(), id));
        return "admin/rights/detail";
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('RIGHTS_EDIT')")
    public String verify(@PathVariable UUID id, @RequestParam String channel, @RequestParam String note,
            @AuthenticationPrincipal AdminPrincipal principal) {
        service.verifyIdentity(principal.adminUserId(), id, channel, note);
        return back(id);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('RIGHTS_EDIT')")
    public String review(@PathVariable UUID id, @AuthenticationPrincipal AdminPrincipal principal) {
        service.startReview(principal.adminUserId(), id);
        return back(id);
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('RIGHTS_APPROVE')")
    public String decide(@PathVariable UUID id, @RequestParam boolean approve, @RequestParam String reason,
            @AuthenticationPrincipal AdminPrincipal principal) {
        service.decide(principal.adminUserId(), id, approve, reason);
        return back(id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('RIGHTS_EDIT')")
    public String complete(@PathVariable UUID id, @RequestParam String result,
            @AuthenticationPrincipal AdminPrincipal principal) {
        service.complete(principal.adminUserId(), id, result);
        return back(id);
    }

    private void populateOptions(AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("recipients", recipients.search(principal.adminUserId(), null, RecipientStatus.ACTIVE));
        model.addAttribute("consentRequests", consentRequests.search(principal.adminUserId(), null));
    }

    private static String back(UUID id) { return "redirect:/admin/rights/" + id; }
}
