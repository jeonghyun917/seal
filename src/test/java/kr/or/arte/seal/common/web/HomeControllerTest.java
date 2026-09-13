package kr.or.arte.seal.common.web;

import kr.or.arte.seal.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(controllers = {HomeController.class, SystemStatusController.class})
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousHomeOpensLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser
    void signedInHomeOpensDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginRedirectPreservesDeploymentContextPath() throws Exception {
        mockMvc.perform(get("/seal/").contextPath("/seal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seal/login"));
    }

    @Test
    void statusShowsConfirmedArchitectureWithoutOtpOrSignature() throws Exception {
        mockMvc.perform(get("/system/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("system/status"))
                .andExpect(model().attributeExists("runtimeVersion", "javaVersion", "databaseVersion"))
                .andExpect(model().attribute("deliveryScope", "이메일 + 일회용 보안 링크"))
                .andExpect(model().attribute("excludedScope", "전자서명·SMS·OTP·실명 본인확인 제외"));
    }
}
