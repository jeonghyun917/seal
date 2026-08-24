package kr.or.arte.seal.request.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.common.config.SecurityConfig;
import kr.or.arte.seal.delivery.application.ConsentDeliveryService;
import kr.or.arte.seal.link.application.IssuedSecureLink;
import kr.or.arte.seal.link.application.SecureLinkManagementService;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.request.application.ConsentRequestManagement;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsentRequestController.class)
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
    void activateShowsIssuedLinkWithoutTokenInRedirectUrl() throws Exception {
        given(links.activate(any(), eq(REQUEST), anyLong()))
                .willReturn(new IssuedSecureLink("opaque-token", Instant.parse("2026-08-25T00:00:00Z")));
        mvc.perform(post("/admin/requests/{id}/activate", REQUEST).param("versionNo", "1")
                        .with(csrf()).with(authentication(admin("CONSENT_REQUEST_ACTIVATE"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/requests/" + REQUEST + "/issued-link"));
    }

    private static UsernamePasswordAuthenticationToken admin(String authority) {
        AdminPrincipal principal = new AdminPrincipal(UUID.randomUUID(), "request-admin",
                "요청 관리자", UUID.randomUUID(), false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                java.util.List.of(new SimpleGrantedAuthority(authority)));
    }
}
