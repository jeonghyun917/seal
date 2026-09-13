package kr.or.arte.seal.policy.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.common.config.SecurityConfig;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityQuery;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityCreateCommand;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityManagementService;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyQuery;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyManagementService;
import kr.or.arte.seal.policy.domain.retention.RetentionStartEvent;
import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
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
    @MockitoBean ProcessingActivityManagementService processingActivityManagementService;
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
    void permittedAdministratorCanOpenProcessingActivityForm() throws Exception {
        mockMvc.perform(get("/admin/processing-activities/new")
                        .with(authentication(admin("PROCESSING_ACTIVITY_EDIT"))))
                .andExpect(status().isOk()).andExpect(view().name("admin/processing-activities/form"))
                .andExpect(model().attributeExists("activityForm", "organizations", "retentionPolicies"));
    }

    @Test
    void processingActivityFormCreatesEverySubmittedDataItemBeyondThree() throws Exception {
        UUID activityId = UUID.fromString("80000000-0000-0000-0000-000000000001");
        given(processingActivityManagementService.create(any())).willReturn(activityId);
        mockMvc.perform(post("/admin/processing-activities")
                        .with(authentication(admin("PROCESSING_ACTIVITY_EDIT"))).with(csrf())
                        .param("activityName", "교육 프로그램 운영")
                        .param("ownerOrganizationUnitId", "30000000-0000-0000-0000-000000000001")
                        .param("processingPurpose", "교육 프로그램 참여자를 등록하고 운영하기 위한 목적입니다.")
                        .param("legalBasisType", "CONSENT")
                        .param("legalBasisDetail", "정보주체의 개인정보 수집 이용 동의")
                        .param("retentionPolicyVersionId", "70000000-0000-0000-0000-000000000001")
                        .param("items[0].itemName", "성명")
                        .param("items[0].necessityReason", "참여자 식별과 출석 확인")
                        .param("items[0].required", "true")
                        .param("items[1].itemName", "이메일")
                        .param("items[1].necessityReason", "동의 요청 메일 발송")
                        .param("items[2].itemName", "소속")
                        .param("items[2].necessityReason", "대상자 소속 확인")
                        .param("items[3].itemName", "전화번호")
                        .param("items[3].necessityReason", "일정 변경 안내"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/processing-activities/" + activityId + "?created"));

        ArgumentCaptor<ProcessingActivityCreateCommand> command =
                ArgumentCaptor.forClass(ProcessingActivityCreateCommand.class);
        verify(processingActivityManagementService).create(command.capture());
        assertThat(command.getValue().dataItems())
                .extracting(item -> item.itemName())
                .containsExactly("성명", "이메일", "소속", "전화번호");
    }

    @Test
    void permittedAdministratorCanOpenRetentionPolicyForm() throws Exception {
        mockMvc.perform(get("/admin/retention-policies/new")
                        .with(authentication(admin("RETENTION_POLICY_EDIT"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/retention-policies/form"))
                .andExpect(model().attribute("startEvents", List.of(
                        RetentionStartEvent.MAIL_FIRST_SENT_AT,
                        RetentionStartEvent.REQUEST_EXPIRES_AT,
                        RetentionStartEvent.CONSENT_SUBMITTED_AT)));
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
