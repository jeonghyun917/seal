package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.application.role.RoleQuery;
import kr.or.arte.seal.admin.application.user.AdminUserManagement;
import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.audit.application.query.AuditEventQuery;
import kr.or.arte.seal.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({AdminUserController.class, RoleController.class, AuditEventController.class})
@Import(SecurityConfig.class)
class AdminReferencePagesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserManagement adminUserManagement;

    @MockitoBean
    private OrganizationManagement organizationManagement;

    @MockitoBean
    private RoleQuery roleQuery;

    @MockitoBean
    private AuditEventQuery auditEventQuery;

    @MockitoBean
    private DatabaseAuthenticationProvider databaseAuthenticationProvider;

    @Test
    void userListRequiresUserViewPermission() throws Exception {
        mockMvc.perform(get("/admin/users").with(authentication(admin("ROLE_OPERATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void permittedUserCanOpenAdministratorList() throws Exception {
        mockMvc.perform(get("/admin/users").with(authentication(admin("USER_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/list"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    void permittedUserCanOpenAdministratorCreationForm() throws Exception {
        mockMvc.perform(get("/admin/users/new").with(authentication(admin("USER_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attributeExists("adminUserForm", "organizations", "roles"));
    }

    @Test
    void administratorCreationRequiresCsrf() throws Exception {
        mockMvc.perform(post("/admin/users").with(authentication(admin("USER_MANAGE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validAdministratorFormCreatesAndRedirects() throws Exception {
        UUID userId = UUID.fromString("20000000-0000-0000-0000-000000000010");
        given(adminUserManagement.create(any())).willReturn(userId);
        mockMvc.perform(post("/admin/users").with(authentication(admin("USER_MANAGE"))).with(csrf())
                        .param("loginId", "new-admin")
                        .param("displayName", "신규 관리자")
                        .param("organizationUnitId", "30000000-0000-0000-0000-000000000001")
                        .param("temporaryPassword", "temporary-123!")
                        .param("roleIds", "40000000-0000-0000-0000-000000000001")
                        .param("organizationScopeIds", "30000000-0000-0000-0000-000000000001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + userId + "?created"));
    }

    @Test
    void permittedUserCanOpenRoleList() throws Exception {
        mockMvc.perform(get("/admin/roles").with(authentication(admin("ROLE_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/roles/list"))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    void permittedUserCanOpenRecentAuditEvents() throws Exception {
        mockMvc.perform(get("/admin/audit-events").with(authentication(admin("AUDIT_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit-events/list"))
                .andExpect(model().attributeExists("events"));
    }

    private static UsernamePasswordAuthenticationToken admin(String authority) {
        AdminPrincipal principal = new AdminPrincipal(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                "test-admin",
                "테스트 관리자",
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                false
        );
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
