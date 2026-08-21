package kr.or.arte.seal.policy.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.common.config.SecurityConfig;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityQuery;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyQuery;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyManagementService;
import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({RetentionPolicyController.class, ProcessingActivityController.class})
@Import(SecurityConfig.class)
class PolicyReferencePagesControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean RetentionPolicyQuery retentionPolicyQuery;
    @MockitoBean RetentionPolicyManagementService retentionPolicyManagementService;
    @MockitoBean OrganizationManagement organizationManagement;
    @MockitoBean ProcessingActivityQuery processingActivityQuery;
    @MockitoBean DatabaseAuthenticationProvider databaseAuthenticationProvider;

    @Test
    void retentionPolicyListRequiresPermission() throws Exception {
        mockMvc.perform(get("/admin/retention-policies").with(authentication(admin("ROLE_OPERATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void permittedAdministratorCanOpenRetentionPolicyList() throws Exception {
        mockMvc.perform(get("/admin/retention-policies").with(authentication(admin("RETENTION_POLICY_VIEW"))))
                .andExpect(status().isOk()).andExpect(view().name("admin/retention-policies/list"))
                .andExpect(model().attributeExists("policies"));
    }

    @Test
    void permittedAdministratorCanOpenProcessingActivityList() throws Exception {
        mockMvc.perform(get("/admin/processing-activities").with(authentication(admin("PROCESSING_ACTIVITY_VIEW"))))
                .andExpect(status().isOk()).andExpect(view().name("admin/processing-activities/list"))
                .andExpect(model().attributeExists("activities"));
    }

    @Test
    void permittedAdministratorCanOpenRetentionPolicyForm() throws Exception {
        mockMvc.perform(get("/admin/retention-policies/new")
                        .with(authentication(admin("RETENTION_POLICY_EDIT"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/retention-policies/form"));
    }

    @Test
    void retentionPolicyCreationRequiresCsrf() throws Exception {
        mockMvc.perform(post("/admin/retention-policies")
                        .with(authentication(admin("RETENTION_POLICY_EDIT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validRetentionPolicyFormCreatesDraftAndRedirects() throws Exception {
        UUID policyId = UUID.fromString("70000000-0000-0000-0000-000000000001");
        given(retentionPolicyManagementService.create(any())).willReturn(policyId);

        mockMvc.perform(post("/admin/retention-policies")
                        .with(authentication(admin("RETENTION_POLICY_EDIT"))).with(csrf())
                        .param("policyCode", "RP-001")
                        .param("policyName", "교육사업 보유정책")
                        .param("ownerOrganizationUnitId", "30000000-0000-0000-0000-000000000001")
                        .param("startEvent", "CONSENT_SUBMITTED_AT")
                        .param("periodValue", "3")
                        .param("periodUnit", "YEAR")
                        .param("gracePeriodDays", "30")
                        .param("destructionMethod", "DELETE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/retention-policies/" + policyId + "?created"));
    }

    private static UsernamePasswordAuthenticationToken admin(String authority) {
        AdminPrincipal principal = new AdminPrincipal(UUID.fromString("20000000-0000-0000-0000-000000000001"),
                "test-admin", "테스트 관리자", UUID.fromString("30000000-0000-0000-0000-000000000001"), false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
