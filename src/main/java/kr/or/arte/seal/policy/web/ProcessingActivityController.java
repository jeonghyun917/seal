package kr.or.arte.seal.policy.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProcessingActivityController {
    private final ProcessingActivityQuery query;
    public ProcessingActivityController(ProcessingActivityQuery query) { this.query = query; }
    @GetMapping("/admin/processing-activities") @PreAuthorize("hasAuthority('PROCESSING_ACTIVITY_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("activities", query.findVisibleActivities(principal.adminUserId()));
        return "admin/processing-activities/list";
    }
}
