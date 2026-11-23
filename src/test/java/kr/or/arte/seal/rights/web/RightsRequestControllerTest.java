package kr.or.arte.seal.rights.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.common.config.SecurityConfig;
import kr.or.arte.seal.recipient.application.RecipientDetail;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import kr.or.arte.seal.request.application.ConsentRequestQueryService;
import kr.or.arte.seal.request.application.ConsentRequestRecord;
import kr.or.arte.seal.rights.application.RightsRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RightsRequestController.class)
@Import({SecurityConfig.class, AdminWebExceptionHandler.class})
class RightsRequestControllerTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private kr.or.arte.seal.admin.config.AdminSessionGuard sessionGuard;
    @Autowired MockMvc mvc;
    @MockitoBean RightsRequestService service;
    @MockitoBean RecipientQueryService recipientQueryService;
    @MockitoBean ConsentRequestQueryService consentRequestQueryService;
    @MockitoBean DatabaseAuthenticationProvider authenticationProvider;

    @Test
    void newFormDoesNotLoadEveryRecipientOrConsentRequest() throws Exception {
        mvc.perform(get("/admin/rights/new")
                        .with(authentication(admin("RIGHTS_EDIT"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rights/form"));

        then(recipientQueryService).should(never()).search(any(), any(), any());
        then(consentRequestQueryService).should(never()).search(any(), any());
    }

    @Test
    void recipientOptionsReturnsOnePageOfActiveRecipients() throws Exception {
        var recipient = new RecipientDetail(UUID.randomUUID(), UUID.randomUUID(), "예술교육팀", "교육팀장",
                "김*술", "ar****@example.com", "권리요청 접수", RecipientStatus.ACTIVE, 0);
        given(recipientQueryService.searchPickerPage(any(), eq("예술"), eq(2), eq(20)))
                .willReturn(new kr.or.arte.seal.common.web.AdminPageResult<>(List.of(recipient), 2, 20, true));

        mvc.perform(get("/admin/rights/recipient-options")
                        .param("keyword", "예술").param("page", "2")
                        .with(authentication(admin("RIGHTS_EDIT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.items[0].recipientId").value(recipient.recipientId().toString()))
                .andExpect(jsonPath("$.items[0].affiliationName").value("예술교육팀"));
    }

    @Test
    void consentRequestOptionsContainOnlyTheSelectedRecipientsRequests() throws Exception {
        UUID recipientId = UUID.randomUUID();
        var matching = new kr.or.arte.seal.request.application.ConsentRequestSummary(UUID.randomUUID(),
                "CR-MATCH", recipientId, kr.or.arte.seal.request.domain.ConsentRequestStatus.ACTIVE,
                java.time.Instant.parse("2026-09-30T09:00:00Z"), java.time.Instant.parse("2026-09-01T09:00:00Z"), 0);
        given(consentRequestQueryService.searchByRecipient(any(), eq(recipientId))).willReturn(List.of(matching));

        mvc.perform(get("/admin/rights/consent-request-options")
                        .param("recipientId", recipientId.toString())
                        .with(authentication(admin("RIGHTS_EDIT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(matching.requestId().toString()))
                .andExpect(jsonPath("$[0].requestCode").value("CR-MATCH"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void createDerivesOrganizationFromScopedRecipientInsteadOfPostedOrganization() throws Exception {
        UUID recipientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID rightsRequestId = UUID.randomUUID();
        given(recipientQueryService.find(any(), eq(recipientId))).willReturn(new RecipientDetail(
                recipientId, organizationId, "홍*동", "h***@example.org", "업무 수행",
                RecipientStatus.ACTIVE, 0));
        given(service.create(eq(organizationId), eq(recipientId), eq(null),
                eq(kr.or.arte.seal.rights.domain.RightsRequestType.ACCESS),
                eq(LocalDate.of(2026, 8, 25)), eq(LocalDate.of(2026, 9, 4)), eq("열람 요청"), any()))
                .willReturn(rightsRequestId);

        mvc.perform(post("/admin/rights")
                        .with(authentication(admin("RIGHTS_EDIT"))).with(csrf())
                        .param("organizationId", UUID.randomUUID().toString())
                        .param("recipientId", recipientId.toString())
                        .param("type", "ACCESS")
                        .param("receivedDate", "2026-08-25")
                        .param("dueDate", "2026-09-04")
                        .param("detail", "열람 요청"))
                .andExpect(status().is3xxRedirection());

        verify(service).create(eq(organizationId), eq(recipientId), eq(null),
                eq(kr.or.arte.seal.rights.domain.RightsRequestType.ACCESS),
                eq(LocalDate.of(2026, 8, 25)), eq(LocalDate.of(2026, 9, 4)), eq("열람 요청"), any());
    }

    @Test
    void createRejectsConsentRequestBelongingToAnotherRecipient() throws Exception {
        UUID recipientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID consentRequestId = UUID.randomUUID();
        given(recipientQueryService.find(any(), eq(recipientId))).willReturn(new RecipientDetail(
                recipientId, organizationId, "홍*동", "h***@example.org", "업무 수행",
                RecipientStatus.ACTIVE, 0));
        ConsentRequestRecord consentRequest = mock(ConsentRequestRecord.class);
        given(consentRequestQueryService.require(any(), eq(consentRequestId))).willReturn(consentRequest);
        given(consentRequest.organizationUnitId()).willReturn(organizationId);
        given(consentRequest.recipientId()).willReturn(UUID.randomUUID());

        mvc.perform(post("/admin/rights")
                        .with(authentication(admin("RIGHTS_EDIT"))).with(csrf())
                        .param("recipientId", recipientId.toString())
                        .param("consentRequestId", consentRequestId.toString())
                        .param("type", "ERASURE")
                        .param("receivedDate", "2026-08-25")
                        .param("dueDate", "2026-09-04")
                        .param("detail", "삭제 요청"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));

        verifyNoInteractions(service);
    }

    @Test
    void invalidCreateFormReturnsFieldErrorsWithoutCallingService() throws Exception {
        mvc.perform(post("/admin/rights")
                        .with(authentication(admin("RIGHTS_EDIT"))).with(csrf())
                        .param("organizationId", UUID.randomUUID().toString())
                        .param("recipientId", UUID.randomUUID().toString())
                        .param("type", "ERASURE")
                        .param("receivedDate", "2026-08-25")
                        .param("dueDate", "2026-08-24")
                        .param("detail", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rights/form"))
                .andExpect(model().attributeHasErrors("rightsRequestForm"));

        verifyNoInteractions(service);
    }

    @Test
    void businessConflictUsesSafe409Page() throws Exception {
        willThrow(new IllegalStateException("internal database state must not be exposed"))
                .given(service).startReview(any(), any());

        mvc.perform(post("/admin/rights/{id}/review", UUID.randomUUID())
                        .with(authentication(admin("RIGHTS_EDIT"))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"))
                .andExpect(model().attributeExists("errorReference"));
    }

    private static UsernamePasswordAuthenticationToken admin(String authority) {
        AdminPrincipal principal = new AdminPrincipal(UUID.randomUUID(), "rights-admin", "권리 관리자",
                UUID.randomUUID(), false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
