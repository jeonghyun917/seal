package kr.or.arte.seal.recipient.importing;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RecipientImportController.class)
@Import(SecurityConfig.class)
class RecipientImportControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean RecipientImportService service;
    @MockitoBean OrganizationManagement organizations;
    @MockitoBean ClamAvProperties clamAv;
    @MockitoBean DatabaseAuthenticationProvider authenticationProvider;

    @Test
    void formRequiresImportPermissionAndRendersForAuthorizedAdmin() throws Exception {
        given(organizations.findVisibleOrganizations(any())).willReturn(List.of());
        given(clamAv.enabled()).willReturn(false);
        mvc.perform(get("/admin/recipients/import")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/recipients/import").with(authentication(admin())))
                .andExpect(status().isOk()).andExpect(view().name("admin/recipients/import-form"));
    }

    @Test
    void uploadRequiresCsrf() throws Exception {
        mvc.perform(post("/admin/recipients/import").with(authentication(admin())))
                .andExpect(status().isForbidden());
    }

    private static UsernamePasswordAuthenticationToken admin() {
        var principal = new AdminPrincipal(UUID.randomUUID(), "import-admin", "반입 관리자",
                UUID.randomUUID(), false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("RECIPIENT_IMPORT")));
    }
}
