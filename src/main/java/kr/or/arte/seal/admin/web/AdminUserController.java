package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.application.user.AdminUserQuery;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminUserController {
    private final AdminUserQuery query;

    public AdminUserController(AdminUserQuery query) { this.query = query; }

    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("users", query.findVisibleUsers(principal.adminUserId()));
        return "admin/users/list";
    }
}
