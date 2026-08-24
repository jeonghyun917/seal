package kr.or.arte.seal.template.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.common.config.SecurityConfig;
import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityQuery;
import kr.or.arte.seal.template.application.ConsentTemplateManagement;
import kr.or.arte.seal.template.application.ConsentTemplateQueryService;
import kr.or.arte.seal.template.application.TemplateNotFoundException;
import kr.or.arte.seal.template.application.TemplatePreviewRenderer;
import kr.or.arte.seal.template.application.TemplateBusinessConflictException;
import kr.or.arte.seal.template.application.ConsentTemplatePage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ConsentTemplateController.class)
@Import({SecurityConfig.class, TemplateWebExceptionHandler.class})
class ConsentTemplateControllerTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("31000000-0000-0000-0000-000000000001");
    private static final UUID ACTIVITY_VERSION_ID = UUID.fromString("32000000-0000-0000-0000-000000000001");
    private static final UUID DATA_ITEM_ID = UUID.fromString("33000000-0000-0000-0000-000000000001");

    @Autowired MockMvc mockMvc;
    @MockitoBean ConsentTemplateManagement management;
    @MockitoBean ConsentTemplateQueryService query;
    @MockitoBean ApprovedProcessingActivityQuery approvedActivityQuery;
    @MockitoBean TemplatePreviewRenderer previewRenderer;
    @MockitoBean DatabaseAuthenticationProvider databaseAuthenticationProvider;

    @Test
    void listRequiresViewPermission() throws Exception {
        mockMvc.perform(get("/admin/templates").with(authentication(admin("ROLE_OPERATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void permittedAdministratorCanOpenList() throws Exception {
        mockMvc.perform(get("/admin/templates")
                        .with(authentication(admin("CONSENT_TEMPLATE_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/templates/list"))
                .andExpect(model().attributeExists("templates", "administrator"));
    }

    @Test
    void createRejectsPostWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/templates")
                        .with(authentication(admin("CONSENT_TEMPLATE_EDIT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validCreateFormRedirectsToDetail() throws Exception {
        given(management.create(any())).willReturn(TEMPLATE_ID);

        mockMvc.perform(post("/admin/templates")
                        .with(authentication(admin("CONSENT_TEMPLATE_EDIT"))).with(csrf())
                        .param("templateCode", "TPL-EDU")
                        .param("templateName", "교육 프로그램 동의서")
                        .param("processingActivityVersionId", ACTIVITY_VERSION_ID.toString())
                        .param("title", "교육 프로그램 개인정보 동의서")
                        .param("introductionHtml", "<p>개인정보 처리 내용을 확인해 주세요.</p>")
                        .param("items[0].code", "COLLECT")
                        .param("items[0].type", "COLLECTION_USE")
                        .param("items[0].title", "개인정보 수집 이용 동의")
                        .param("items[0].required", "true")
                        .param("items[0].purpose", "교육 프로그램 운영 목적")
                        .param("items[0].retentionText", "동의 제출 후 3년")
                        .param("items[0].refusalNotice", "동의를 거부하면 신청이 제한될 수 있습니다.")
                        .param("items[0].processingDataItemIds", DATA_ITEM_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/templates/" + TEMPLATE_ID + "?created"));
    }

    @Test
    void outOfScopeDetailIsNotFound() throws Exception {
        given(query.requireScoped(TEMPLATE_ID, principal().adminUserId()))
                .willThrow(new TemplateNotFoundException());

        mockMvc.perform(get("/admin/templates/{id}", TEMPLATE_ID)
                        .with(authentication(admin("CONSENT_TEMPLATE_VIEW"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void workflowConflictReturnsLatestDetailWithConflictStatus() throws Exception {
        UUID versionId = UUID.randomUUID();
        willThrow(new TemplateBusinessConflictException("다른 사용자가 상태를 변경했습니다."))
                .given(management).publish(TEMPLATE_ID, versionId, principal().adminUserId());
        given(query.requireScoped(TEMPLATE_ID, principal().adminUserId()))
                .willReturn(new ConsentTemplatePage(TEMPLATE_ID, "TPL-EDU", "교육 동의서",
                        UUID.randomUUID(), "교육 업무", "ACTIVE", null, 1, List.of()));

        mockMvc.perform(post("/admin/templates/{templateId}/versions/{versionId}/publish",
                        TEMPLATE_ID, versionId)
                        .with(authentication(admin("CONSENT_TEMPLATE_PUBLISH"))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(view().name("admin/templates/detail"))
                .andExpect(model().attributeExists("template", "conflictMessage"));
    }

    private static UsernamePasswordAuthenticationToken admin(String... authorities) {
        return UsernamePasswordAuthenticationToken.authenticated(principal(), null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }

    private static AdminPrincipal principal() {
        return new AdminPrincipal(UUID.fromString("30000000-0000-0000-0000-000000000001"),
                "template-admin", "템플릿 관리자",
                UUID.fromString("30000000-0000-0000-0000-000000000002"), false);
    }
}
