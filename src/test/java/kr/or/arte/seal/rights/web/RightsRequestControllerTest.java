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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RightsRequestController.class)
@Import({SecurityConfig.class, AdminWebExceptionHandler.class})
class RightsRequestControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean RightsRequestService service;
    @MockitoBean RecipientQueryService recipientQueryService;
    @MockitoBean ConsentRequestQueryService consentRequestQueryService;
    @MockitoBean DatabaseAuthenticationProvider authenticationProvider;

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
