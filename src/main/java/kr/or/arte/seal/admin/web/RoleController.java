package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.application.role.RoleQuery;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RoleController {
    private final RoleQuery query;

    public RoleController(RoleQuery query) { this.query = query; }

    @GetMapping("/admin/roles")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("roles", query.findRoles());
        return "admin/roles/list";
    }
}
