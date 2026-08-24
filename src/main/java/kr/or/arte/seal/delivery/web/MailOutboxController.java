package kr.or.arte.seal.delivery.web;

import jakarta.servlet.http.HttpServletResponse;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.delivery.application.MailOutboxQueryService;
import kr.or.arte.seal.delivery.application.MailOutboxStatus;
import kr.or.arte.seal.delivery.config.MailRelayProperties;
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
    private final MailRelayProperties mailProperties;

    public MailOutboxController(MailOutboxQueryService query, MailRelayProperties mailProperties) {
        this.query = query;
        this.mailProperties = mailProperties;
    }

    @GetMapping("/admin/mail-outbox")
    @PreAuthorize("hasAuthority('MAIL_VIEW')")
    public String list(@RequestParam(required = false) MailOutboxStatus status,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("mailItems", query.search(principal.adminUserId(), status));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("mailEnabled", mailProperties.enabled());
        return "admin/mail/list";
    }

    @GetMapping("/admin/mail-outbox/{outboxId}")
    @PreAuthorize("hasAuthority('MAIL_VIEW')")
    public String detail(@PathVariable UUID outboxId, @AuthenticationPrincipal AdminPrincipal principal,
            Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("mailItem", query.require(principal.adminUserId(), outboxId));
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
        return query.require(principal.adminUserId(), outboxId).html();
    }

    @PostMapping("/admin/mail-outbox/{outboxId}/retry")
    @PreAuthorize("hasAuthority('MAIL_RETRY')")
    public String retry(@PathVariable UUID outboxId, @AuthenticationPrincipal AdminPrincipal principal) {
        query.requeue(principal.adminUserId(), outboxId);
        return "redirect:/admin/mail-outbox/" + outboxId;
    }
}
