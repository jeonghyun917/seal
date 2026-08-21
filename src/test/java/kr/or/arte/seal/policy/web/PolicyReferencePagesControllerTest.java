package kr.or.arte.seal.policy.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.common.config.SecurityConfig;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityQuery;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyQuery;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({RetentionPolicyController.class, ProcessingActivityController.class})
@Import(SecurityConfig.class)
class PolicyReferencePagesControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean RetentionPolicyQuery retentionPolicyQuery;
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

    private static UsernamePasswordAuthenticationToken admin(String authority) {
        AdminPrincipal principal = new AdminPrincipal(UUID.fromString("20000000-0000-0000-0000-000000000001"),
                "test-admin", "테스트 관리자", UUID.fromString("30000000-0000-0000-0000-000000000001"), false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
