package kr.or.arte.seal.recipient.importing;

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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RecipientImportController.class)
@Import(SecurityConfig.class)
class RecipientImportControllerTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private kr.or.arte.seal.admin.config.AdminSessionGuard sessionGuard;
    private static final UUID INSTITUTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Autowired MockMvc mvc;
    @MockitoBean RecipientImportService service;
    @MockitoBean ClamAvProperties clamAv;
    @MockitoBean DatabaseAuthenticationProvider authenticationProvider;

    @Test
    void formRequiresImportPermissionAndRendersForAuthorizedAdmin() throws Exception {
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

    @Test
    void uploadAlwaysUsesInstitutionOrganization() throws Exception {
        UUID batchId = UUID.randomUUID();
        var preview = new RecipientImportPreview(batchId, RecipientImportStatus.VALIDATED,
                MalwareScanResult.CLEAN, 1, 1, 0, 0, 0, java.time.Instant.now(), List.of());
        given(service.validate(any(), any(), any())).willReturn(preview);
        var file = new MockMultipartFile("file", "recipients.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{'P', 'K', 1, 2});

        mvc.perform(multipart("/admin/recipients/import").file(file)
                        .param("organizationUnitId", UUID.randomUUID().toString())
                        .with(authentication(admin())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/recipients/import/" + batchId));

        verify(service).validate(any(), argThat(INSTITUTION_ID::equals), any());
    }

    @Test
    void downloadsTemplateFromExtensionlessRoute() throws Exception {
        var result = mvc.perform(get("/admin/recipients/import/template").with(authentication(admin())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=recipient-import-template.xlsx"))
                .andReturn();

        assertThat(result.getResponse().getContentType())
                .startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(result.getResponse().getContentLengthLong()).isPositive();
        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }
    private static UsernamePasswordAuthenticationToken admin() {
        var principal = new AdminPrincipal(UUID.randomUUID(), "import-admin", "반입 관리자",
                UUID.randomUUID(), false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("RECIPIENT_IMPORT")));
    }
}
