package kr.or.arte.seal.common.web;

import kr.or.arte.seal.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        controllers = {HomeController.class, SystemStatusController.class},
        properties = "spring.security.user.password=test-only")
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeRendersJspView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("serviceName", "개인정보 전자동의·증적 관리시스템"));
    }

    @Test
    void statusShowsConfirmedArchitectureWithoutOtpOrSignature() throws Exception {
        mockMvc.perform(get("/system/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("system/status"))
                .andExpect(model().attribute("databaseVersion", "PostgreSQL 16.14"))
                .andExpect(model().attribute("deliveryScope", "이메일 + 일회용 보안 링크"))
                .andExpect(model().attribute("excludedScope", "전자서명·OTP 제외"));
    }
}
