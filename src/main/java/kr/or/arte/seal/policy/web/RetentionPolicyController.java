package kr.or.arte.seal.policy.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RetentionPolicyController {
    private final RetentionPolicyQuery query;
    public RetentionPolicyController(RetentionPolicyQuery query) { this.query = query; }
    @GetMapping("/admin/retention-policies") @PreAuthorize("hasAuthority('RETENTION_POLICY_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("policies", query.findVisiblePolicies(principal.adminUserId()));
        return "admin/retention-policies/list";
    }
}
