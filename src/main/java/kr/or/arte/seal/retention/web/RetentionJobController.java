package kr.or.arte.seal.retention.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.retention.application.RetentionJobService;
import kr.or.arte.seal.retention.domain.RetentionAnchorType;
import kr.or.arte.seal.retention.domain.RetentionJobStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping({"/admin/retention", "/admin/retention-jobs"})
public class RetentionJobController {
    private final RetentionJobService service;

    public RetentionJobController(RetentionJobService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RETENTION_VIEW')")
    public String list(@RequestParam(required = false) RetentionJobStatus status,
                       @RequestParam(defaultValue = "1") int page, @AuthenticationPrincipal AdminPrincipal principal,
                       Model model) {
        model.addAttribute("administrator", principal);
        var pageResult=service.searchPage(principal.adminUserId(),status,page,20); model.addAttribute("items",pageResult.items()); model.addAttribute("pageResult",pageResult);
        model.addAttribute("activeHolds", service.activeLegalHolds(principal.adminUserId()));
        model.addAttribute("selectedStatus", status);
        return "admin/retention/list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RETENTION_VIEW')")
    public String detail(@PathVariable UUID id,
                         @AuthenticationPrincipal AdminPrincipal principal,
                         Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("item", service.require(principal.adminUserId(), id));
        return "admin/retention/detail";
    }

    @PostMapping("/anchors")
    @PreAuthorize("hasAuthority('RETENTION_MANAGE')")
    public String recordAnchor(@RequestParam UUID consentRequestId,
                               @RequestParam RetentionAnchorType anchorType,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anchorDate,
                               @AuthenticationPrincipal AdminPrincipal principal) {
        service.recordAnchor(principal.adminUserId(), consentRequestId, anchorType, anchorDate);
        return "redirect:/admin/retention";
    }

    @PostMapping("/holds")
    @PreAuthorize("hasAuthority('RETENTION_MANAGE')")
    public String placeHold(@RequestParam UUID consentRequestId,
                            @RequestParam String reasonCode,
                            @RequestParam String reasonText,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                            @AuthenticationPrincipal AdminPrincipal principal) {
        service.placeLegalHold(principal.adminUserId(), consentRequestId, reasonCode, reasonText, expiresOn);
        return "redirect:/admin/retention";
    }

    @PostMapping("/holds/{holdId}/release")
    @PreAuthorize("hasAuthority('RETENTION_MANAGE')")
    public String releaseHold(@PathVariable UUID holdId,
                              @AuthenticationPrincipal AdminPrincipal principal) {
        service.releaseLegalHold(principal.adminUserId(), holdId);
        return "redirect:/admin/retention";
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('RETENTION_MANAGE')")
    public String submit(@PathVariable UUID id, @AuthenticationPrincipal AdminPrincipal principal) {
        service.submit(principal.adminUserId(), id);
        return back(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('RETENTION_APPROVE')")
    public String approve(@PathVariable UUID id, @AuthenticationPrincipal AdminPrincipal principal) {
        service.approve(principal.adminUserId(), id);
        return back(id);
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('RETENTION_EXECUTE')")
    public String execute(@PathVariable UUID id, @RequestParam UUID idempotencyKey,
                          @AuthenticationPrincipal AdminPrincipal principal) {
        service.execute(principal.adminUserId(), id, idempotencyKey);
        return back(id);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('RETENTION_CONFIRM')")
    public String confirm(@PathVariable UUID id, @AuthenticationPrincipal AdminPrincipal principal) {
        service.confirm(principal.adminUserId(), id);
        return back(id);
    }

    private static String back(UUID id) {
        return "redirect:/admin/retention/" + id;
    }
}
