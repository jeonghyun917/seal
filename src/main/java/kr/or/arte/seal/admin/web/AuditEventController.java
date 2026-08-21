package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.audit.application.query.AuditEventQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditEventController {
    private final AuditEventQuery query;

    public AuditEventController(AuditEventQuery query) { this.query = query; }

    @GetMapping("/admin/audit-events")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("events", query.findRecentVisibleEvents(principal.adminUserId()));
        return "admin/audit-events/list";
    }
}
