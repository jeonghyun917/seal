package kr.or.arte.seal.request.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.common.config.SecurityConfig;
import kr.or.arte.seal.delivery.application.ConsentDeliveryService;
import kr.or.arte.seal.link.application.IssuedSecureLink;
import kr.or.arte.seal.link.application.SecureLinkManagementService;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.request.application.ConsentRequestManagement;
import kr.or.arte.seal.request.application.ConsentRequestRecord;
import kr.or.arte.seal.template.application.PublishedConsentTemplateOption;
import kr.or.arte.seal.request.application.ConsentRequestQueryService;
import kr.or.arte.seal.template.application.PublishedConsentTemplateQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = ConsentRequestController.class, properties = "seal.public-base-url=https://consent.arte.or.kr")
@Import(SecurityConfig.class)
class ConsentRequestControllerTest {
    private static final UUID REQUEST = UUID.randomUUID();
    @Autowired MockMvc mvc;
    @MockitoBean ConsentRequestManagement management;
    @MockitoBean ConsentRequestQueryService query;
    @MockitoBean SecureLinkManagementService links;
    @MockitoBean ConsentDeliveryService delivery;
    @MockitoBean RecipientQueryService recipients;
    @MockitoBean PublishedConsentTemplateQuery templates;
    @MockitoBean DatabaseAuthenticationProvider databaseAuthenticationProvider;

    @Test
    void createFormDoesNotLoadEveryRecipientBeforePickerIsOpened() throws Exception {
        mvc.perform(get("/admin/requests/new")
                        .with(authentication(admin("CONSENT_REQUEST_EDIT"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/requests/form"));

        then(recipients).should(never()).search(any(), any(), any());
    }

    @Test
    void recipientOptionsReturnsPagedJsonForPicker() throws Exception {
        var recipient = new kr.or.arte.seal.recipient.application.RecipientDetail(
                UUID.randomUUID(), UUID.randomUUID(), "예술교육팀", "교육팀장",
                "김*술", "ar****@example.com", "등록 근거",
                kr.or.arte.seal.recipient.domain.RecipientStatus.ACTIVE, 0);
        given(recipients.searchPickerPage(any(), eq("예술"), eq(2), eq(20)))
                .willReturn(new kr.or.arte.seal.common.web.AdminPageResult<>(java.util.List.of(recipient), 2, 20, true));

        mvc.perform(get("/admin/requests/recipient-options")
                        .param("keyword", "예술").param("page", "2")
                        .with(authentication(admin("CONSENT_REQUEST_EDIT"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.page").value(2))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.hasNext").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].affiliationName").value("예술교육팀"));
    }

    @Test
    void createRejectsRequestWhenNoRecipientsAreSelected() throws Exception {
        mvc.perform(post("/admin/requests")
                        .param("templateVersionId", UUID.randomUUID().toString())
                        .param("expiresAt", "2026-09-10T18:00")
                        .param("idempotencyKey", UUID.randomUUID().toString())
                        .with(csrf()).with(authentication(admin("CONSENT_REQUEST_EDIT"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/requests/form"))
                .andExpect(model().attributeHasFieldErrors("requestForm", "recipientIds"));

        verifyNoInteractions(management);
    }

    @Test
    void requestDetailIncludesReadableTemplateVersionName() throws Exception {
        UUID templateVersionId = UUID.randomUUID();
        ConsentRequestRecord request = mock(ConsentRequestRecord.class);
        PublishedConsentTemplateOption option = new PublishedConsentTemplateOption(
                templateVersionId, "CONSENT-EDU", "교육 프로그램 참여 동의서", "교육 프로그램", 3);
        given(request.recipientId()).willReturn(UUID.randomUUID());
        given(request.templateVersionId()).willReturn(templateVersionId);
        given(query.require(any(), eq(REQUEST))).willReturn(request);
        given(templates.findVersionOption(any(), eq(templateVersionId)))
                .willReturn(java.util.Optional.of(option));

        mvc.perform(get("/admin/requests/{id}", REQUEST)
                        .with(authentication(admin("CONSENT_REQUEST_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/requests/detail"))
                .andExpect(model().attribute("requestTemplate", option));
    }

    @Test
    void snapshotPreviewProvidesReadableViewAlongsideRawEvidence() throws Exception {
        String snapshotJson = """
                {"institution":"한국문화예술교육진흥원","requestCode":"CR-TEST","title":"읽기 쉬운 동의서",
                 "introductionHtml":"<p>안내</p>","processingActivityName":"교육 운영","processingPurpose":"교육 안내",
                 "retentionDisplayText":"1년","expiresAt":"2026-09-10T09:00:00Z","withdrawalNotice":"철회 가능",
                 "processingActivityVersionId":"activity-v1","retentionPolicyVersionId":"retention-v1",
                 "templateVersionId":"template-v1","templateContentHash":"hash","items":[]}
                """;
        given(query.requireSnapshotJson(any(), eq(REQUEST))).willReturn(snapshotJson);

        mvc.perform(get("/admin/requests/{id}/preview", REQUEST)
                        .with(authentication(admin("CONSENT_REQUEST_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/requests/preview"))
                .andExpect(model().attribute("snapshotJson", snapshotJson))
                                .andExpect(model().attributeExists("snapshotView"));
    }
    @Test
    void directActivateEndpointIsNotExposed() throws Exception {
        mvc.perform(post("/admin/requests/{id}/activate", REQUEST).param("versionNo", "1")
                        .with(csrf()).with(authentication(admin("CONSENT_REQUEST_ACTIVATE"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void directReissueEndpointIsNotExposed() throws Exception {
        mvc.perform(post("/admin/requests/{id}/reissue", REQUEST).param("versionNo", "1")
                        .with(csrf()).with(authentication(admin("CONSENT_REQUEST_LINK_REISSUE"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void redeliveryAcceptsReadableKoreanScheduleFormat() throws Exception {
        given(delivery.reissueAndQueue(any(), eq(REQUEST), anyLong(), any(), any()))
                .willReturn(new kr.or.arte.seal.delivery.application.DeliveryIssueResult(UUID.randomUUID(),
                        new IssuedSecureLink("opaque-token", Instant.parse("2026-08-31T00:00:00Z")),
                        kr.or.arte.seal.delivery.application.MailOutboxStatus.QUEUED));

        mvc.perform(post("/admin/requests/{id}/redeliver", REQUEST)
                        .param("versionNo", "1")
                        .param("scheduledAt", "2026-08-31 09:00")
                        .with(csrf()).with(authentication(admin("MAIL_SEND"))))
                .andExpect(status().is3xxRedirection());

        org.mockito.BDDMockito.then(delivery).should().reissueAndQueue(any(), eq(REQUEST), anyLong(),
                eq("https://consent.arte.or.kr"), eq(Instant.parse("2026-08-31T00:00:00Z")));
    }

    @Test
    void deliveryUsesConfiguredPublicBaseUrlInsteadOfRequestHost() throws Exception {
        given(delivery.activateAndQueue(any(), eq(REQUEST), anyLong(), any(), any()))
                .willReturn(new kr.or.arte.seal.delivery.application.DeliveryIssueResult(UUID.randomUUID(),
                        new IssuedSecureLink("opaque-token", Instant.parse("2026-08-25T00:00:00Z")),
                        kr.or.arte.seal.delivery.application.MailOutboxStatus.QUEUED));

        mvc.perform(post("/admin/requests/{id}/deliver", REQUEST).param("versionNo", "1")
                        .header("Host", "malicious.example").with(csrf()).with(authentication(admin("MAIL_SEND"))))
                .andExpect(status().is3xxRedirection());

        org.mockito.BDDMockito.then(delivery).should().activateAndQueue(any(), eq(REQUEST), anyLong(),
                eq("https://consent.arte.or.kr"), any());
    }

    private static UsernamePasswordAuthenticationToken admin(String authority) {
        AdminPrincipal principal = new AdminPrincipal(UUID.randomUUID(), "request-admin",
                "요청 관리자", UUID.randomUUID(), false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                java.util.List.of(new SimpleGrantedAuthority(authority)));
    }
}
