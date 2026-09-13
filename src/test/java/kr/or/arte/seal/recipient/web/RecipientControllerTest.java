package kr.or.arte.seal.recipient.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.common.web.AdminPageResult;
import kr.or.arte.seal.common.config.SecurityConfig;
import kr.or.arte.seal.recipient.application.RecipientManagementService;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RecipientController.class)
@Import(SecurityConfig.class)
class RecipientControllerTest {
    private static final UUID RECIPIENT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ORG_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID INSTITUTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Autowired MockMvc mockMvc;
    @MockitoBean RecipientManagementService management;
    @MockitoBean RecipientQueryService query;
    @MockitoBean DatabaseAuthenticationProvider databaseAuthenticationProvider;

    @Test
    void listRequiresRecipientViewPermission() throws Exception {
        mockMvc.perform(get("/admin/recipients").with(authentication(admin("ROLE_OPERATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void permittedAdministratorCanOpenList() throws Exception {
        given(query.searchPage(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .willReturn(AdminPageResult.of(java.util.List.of(), 1, 20));
        mockMvc.perform(get("/admin/recipients").with(authentication(admin("RECIPIENT_VIEW"))))
                .andExpect(status().isOk()).andExpect(view().name("admin/recipients/list"))
                .andExpect(model().attributeExists("recipients", "administrator"));
    }

    @Test
    void listPassesEmailAndCollectionBasisSearchTerms() throws Exception {
        given(query.searchPage(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .willReturn(AdminPageResult.of(java.util.List.of(), 1, 20));

        mockMvc.perform(get("/admin/recipients")
                        .param("email", "hong@example.com")
                        .param("collectionBasis", "교육 참여 신청서")
                        .with(authentication(admin("RECIPIENT_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("email", "hong@example.com"))
                .andExpect(model().attribute("collectionBasis", "교육 참여 신청서"));

        verify(query).searchPage(any(), eq("hong@example.com"), eq("교육 참여 신청서"),
                any(), eq(1), eq(20));
    }

    @Test
    void createRejectsPostWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/recipients").with(authentication(admin("RECIPIENT_EDIT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validCreateRedirectsToMaskedDetail() throws Exception {
        given(management.create(any())).willReturn(RECIPIENT_ID);
        mockMvc.perform(post("/admin/recipients").with(authentication(admin("RECIPIENT_EDIT"))).with(csrf())
                        .param("organizationUnitId", ORG_ID.toString()).param("affiliationName", "외부회사")
                        .param("jobTitle", "교육팀장").param("name", "홍길동")
                        .param("email", "hong@example.com").param("collectionBasis", "교육 참여 신청서 수집"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/recipients/" + RECIPIENT_ID + "?created"));

        verify(management).create(argThat(command -> INSTITUTION_ID.equals(command.organizationUnitId())
                && "교육팀장".equals(command.jobTitle())));
    }

    private static UsernamePasswordAuthenticationToken admin(String... authorities) {
        return UsernamePasswordAuthenticationToken.authenticated(principal(), null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }

    private static AdminPrincipal principal() {
        return new AdminPrincipal(UUID.fromString("40000000-0000-0000-0000-000000000003"),
                "recipient-admin", "대상자 관리자", ORG_ID, false);
    }
}
