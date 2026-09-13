package kr.or.arte.seal.delivery.web;

import jakarta.servlet.http.HttpServletResponse;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.delivery.application.MailOutboxQueryService;
import kr.or.arte.seal.delivery.application.MailOutboxStatus;
import kr.or.arte.seal.delivery.config.MailRelayProperties;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.request.application.ConsentRequestQueryService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

@Controller
public class MailOutboxController {
    private final MailOutboxQueryService query;
    private final ConsentRequestQueryService requestQuery;
    private final RecipientQueryService recipientQuery;
    private final MailRelayProperties mailProperties;

    public MailOutboxController(MailOutboxQueryService query, ConsentRequestQueryService requestQuery,
            RecipientQueryService recipientQuery, MailRelayProperties mailProperties) {
        this.query = query;
        this.requestQuery = requestQuery;
        this.recipientQuery = recipientQuery;
        this.mailProperties = mailProperties;
    }

    @GetMapping("/admin/mail-outbox")
    @PreAuthorize("hasAuthority('MAIL_VIEW')")
    public String list(@RequestParam(required = false) MailOutboxStatus status,
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "1") int page, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        var pageResult=query.searchPage(principal.adminUserId(),status,q,page,20);
        var items=pageResult.items().stream().map(item->{var request=requestQuery.require(principal.adminUserId(),item.requestId());return new MailListItem(item,request,recipientQuery.find(principal.adminUserId(),request.recipientId()));}).toList();
        model.addAttribute("mailItems",items); model.addAttribute("pageResult",pageResult); model.addAttribute("q",q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("mailEnabled", mailProperties.enabled());
        return "admin/mail/list";
    }

    public record MailListItem(kr.or.arte.seal.delivery.application.MailOutboxSummary summary,
            kr.or.arte.seal.request.application.ConsentRequestRecord request,
            kr.or.arte.seal.recipient.application.RecipientDetail recipient) { }

    @GetMapping("/admin/mail-outbox/{outboxId}")
    @PreAuthorize("hasAuthority('MAIL_VIEW')")
    public String detail(@PathVariable UUID outboxId, @AuthenticationPrincipal AdminPrincipal principal,
            Model model) {
        model.addAttribute("administrator", principal);
        var mailItem = query.require(principal.adminUserId(), outboxId);
        var request = requestQuery.require(principal.adminUserId(), mailItem.requestId());
        model.addAttribute("mailItem", mailItem);
        model.addAttribute("requestCode", request.requestCode());
        model.addAttribute("recipient", recipientQuery.find(principal.adminUserId(), request.recipientId()));
        MailSchedulePresentation.from(mailItem)
                .ifPresent(scheduleView -> model.addAttribute("scheduleView", scheduleView));
        return "admin/mail/detail";
    }

    @GetMapping(value = "/admin/mail-outbox/{outboxId}/preview", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('MAIL_VIEW')")
    @ResponseBody
    public String preview(@PathVariable UUID outboxId, @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'");
        return MailPreviewSanitizer.sanitize(query.require(principal.adminUserId(), outboxId).html());
    }

    @PostMapping("/admin/mail-outbox/{outboxId}/retry")
    @PreAuthorize("hasAuthority('MAIL_RETRY')")
    public String retry(@PathVariable UUID outboxId, @AuthenticationPrincipal AdminPrincipal principal) {
        query.requeue(principal.adminUserId(), outboxId);
        return "redirect:/admin/mail-outbox/" + outboxId;
    }
}
