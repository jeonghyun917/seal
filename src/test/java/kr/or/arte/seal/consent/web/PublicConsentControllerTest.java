package kr.or.arte.seal.consent.web;

import kr.or.arte.seal.admin.config.DatabaseAuthenticationProvider;
import kr.or.arte.seal.common.config.SecurityConfig;
import kr.or.arte.seal.consent.application.ConsentSubmissionService;
import kr.or.arte.seal.consent.application.PublicConsentEntry;
import kr.or.arte.seal.consent.application.PublicConsentEntryService;
import kr.or.arte.seal.consent.application.PublicConsentEntryStatus;
import kr.or.arte.seal.consent.application.PublicConsentFlow;
import kr.or.arte.seal.consent.application.PublicConsentQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PublicConsentController.class)
@Import(SecurityConfig.class)
class PublicConsentControllerTest {
    private static final String TOKEN = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String FLOW = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";
    @Autowired MockMvc mvc;
    @MockitoBean PublicConsentEntryService entry;
    @MockitoBean PublicConsentQueryService query;
    @MockitoBean ConsentSubmissionService submissions;
    @MockitoBean DatabaseAuthenticationProvider authenticationProvider;

    @Test
    void getOnlyInspectsAndSetsNoStoreHeaders() throws Exception {
        given(entry.inspect(TOKEN)).willReturn(new PublicConsentEntry(PublicConsentEntryStatus.VALID));

        mvc.perform(get("/consent/{token}", TOKEN))
                .andExpect(status().isOk())
                .andExpect(view().name("consent/entry"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        verify(entry).inspect(TOKEN);
        verify(entry, never()).start(TOKEN);
    }

    @Test
    void startRequiresCsrfAndRedirectsToTokenFreeFlow() throws Exception {
        given(entry.start(TOKEN)).willReturn(new PublicConsentFlow(FLOW, UUID.randomUUID(),
                Instant.parse("2026-08-24T01:00:00Z")));

        mvc.perform(post("/consent/{token}/start", TOKEN)).andExpect(status().isForbidden());
        mvc.perform(post("/consent/{token}/start", TOKEN).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/consent/respond/" + FLOW))
                .andExpect(request().sessionAttribute("SEAL_PUBLIC_FLOW", FLOW));
    }
}
