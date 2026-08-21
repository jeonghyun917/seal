package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrganizationController {

    private final OrganizationManagement organizationManagement;

    public OrganizationController(OrganizationManagement organizationManagement) {
        this.organizationManagement = organizationManagement;
    }

    @GetMapping("/admin/organizations")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
    public String list(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("organizations",
                organizationManagement.findVisibleOrganizations(principal.adminUserId()));
        return "admin/organizations/list";
    }
}
