package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
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

@WebMvcTest(OrganizationController.class)
@Import(SecurityConfig.class)
class OrganizationControllerTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private kr.or.arte.seal.admin.config.AdminSessionGuard sessionGuard;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationManagement organizationManagement;

    @MockitoBean
    private DatabaseAuthenticationProvider databaseAuthenticationProvider;

    @Test
    void organizationListRequiresViewPermission() throws Exception {
        mockMvc.perform(get("/admin/organizations")
                        .with(authentication(authenticationWithoutPermission())))
                .andExpect(status().isForbidden());
    }

    @Test
    void permittedAdministratorCanOpenVisibleOrganizationList() throws Exception {
        mockMvc.perform(get("/admin/organizations")
                        .with(authentication(authenticationWithPermission())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/organizations/list"))
                .andExpect(model().attributeExists("organizations", "administrator"));
    }

    @Test
    void permittedAdministratorCanOpenOrganizationForm() throws Exception {
        mockMvc.perform(get("/admin/organizations/new")
                        .with(authentication(authenticationWithManagePermission())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/organizations/form"))
                .andExpect(model().attributeExists("organizationForm", "parentOrganizations"));
    }

    @Test
    void organizationCreationRequiresCsrf() throws Exception {
        mockMvc.perform(post("/admin/organizations")
                        .with(authentication(authenticationWithManagePermission())))
                .andExpect(status().isForbidden());
    }

    @Test
    void validOrganizationFormCreatesAndRedirects() throws Exception {
        UUID organizationId = UUID.fromString("30000000-0000-0000-0000-000000000010");
        given(organizationManagement.create(any())).willReturn(organizationId);

        mockMvc.perform(post("/admin/organizations")
                        .with(authentication(authenticationWithManagePermission())).with(csrf())
                        .param("organizationCode", "TEAM-A")
                        .param("organizationName", "교육사업팀")
                        .param("parentId", "30000000-0000-0000-0000-000000000001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/organizations/" + organizationId + "?created"));
    }

    private static UsernamePasswordAuthenticationToken authenticationWithoutPermission() {
        return adminAuthentication(List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
    }

    private static UsernamePasswordAuthenticationToken authenticationWithPermission() {
        return adminAuthentication(List.of(new SimpleGrantedAuthority("ORGANIZATION_VIEW")));
    }

    private static UsernamePasswordAuthenticationToken authenticationWithManagePermission() {
        return adminAuthentication(List.of(new SimpleGrantedAuthority("ORGANIZATION_MANAGE")));
    }

    private static UsernamePasswordAuthenticationToken adminAuthentication(List<SimpleGrantedAuthority> authorities) {
        AdminPrincipal principal = new AdminPrincipal(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                "test-admin",
                "테스트 관리자",
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                false
        );
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }
}
