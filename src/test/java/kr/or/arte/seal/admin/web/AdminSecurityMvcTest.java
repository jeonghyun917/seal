package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.application.AdminPasswordChangeService;
import kr.or.arte.seal.admin.application.PasswordChangeResult;
import kr.or.arte.seal.admin.application.PasswordChangeStatus;
import kr.or.arte.seal.admin.config.AdminPrincipal;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {
        LoginController.class,
        AdminHomeController.class,
        PasswordChangeController.class
})
@Import(SecurityConfig.class)
class AdminSecurityMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPasswordChangeService passwordChangeService;

    @Test
    void anonymousAdministratorRequestRedirectsToCustomLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void loginPageIsPublicAndUsesCustomView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    @Test
    void csrfLessPasswordChangeIsRejected() throws Exception {
        mockMvc.perform(post("/account/password/change")
                        .with(user("admin").roles("SYSTEM_ADMIN"))
                        .param("currentPassword", "current-password")
                        .param("newPassword", "new-password-12")
                        .param("newPasswordConfirmation", "new-password-12"))
                .andExpect(status().isForbidden());
    }

    @Test
    void principalRequiringPasswordChangeCannotOpenAdminHome() throws Exception {
        mockMvc.perform(get("/admin").with(authentication(adminAuthentication(true))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/password/change"));
    }

    @Test
    void normalAdministratorCanOpenAdminHome() throws Exception {
        mockMvc.perform(get("/admin").with(authentication(adminAuthentication(false))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/home"));
    }

    @Test
    void forcedChangePageItselfRemainsAccessible() throws Exception {
        mockMvc.perform(get("/account/password/change").with(authentication(adminAuthentication(true))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/password-change"));
    }

    @Test
    void successfulPasswordChangeEndsAtFreshLoginPage() throws Exception {
        UUID adminUserId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        given(passwordChangeService.change(
                adminUserId,
                "current-password",
                "new-password-12",
                "new-password-12"
        )).willReturn(PasswordChangeResult.of(PasswordChangeStatus.CHANGED));

        mockMvc.perform(post("/account/password/change")
                        .with(authentication(adminAuthentication(true)))
                        .with(csrf())
                        .param("currentPassword", "current-password")
                        .param("newPassword", "new-password-12")
                        .param("newPasswordConfirmation", "new-password-12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordChanged"));
    }

    private static UsernamePasswordAuthenticationToken adminAuthentication(boolean mustChangePassword) {
        AdminPrincipal principal = new AdminPrincipal(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "admin",
                "관리자",
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                mustChangePassword
        );
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))
        );
    }
}
