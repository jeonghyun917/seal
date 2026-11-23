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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PublicConsentController.class)
@Import(SecurityConfig.class)
class PublicConsentControllerTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private kr.or.arte.seal.admin.config.AdminSessionGuard sessionGuard;
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

    @Test
    void respondRestoresDecisionsKeptInTheSameSession() throws Exception {
        UUID itemId = UUID.randomUUID();
        var page = page(itemId);
        Map<UUID, kr.or.arte.seal.consent.application.ConsentDecision> decisions = Map.of(itemId, kr.or.arte.seal.consent.application.ConsentDecision.AGREE);
        given(query.load(FLOW)).willReturn(page);
        mvc.perform(get("/consent/respond/{flow}", FLOW).sessionAttr("SEAL_PUBLIC_FLOW", FLOW).sessionAttr("SEAL_PUBLIC_DECISIONS", decisions))
                .andExpect(status().isOk()).andExpect(model().attribute("decisions", decisions));
    }

    @Test
    void completionExposesOnlyTheReceiptSavedByTheMatchingSession() throws Exception {
        UUID itemId = UUID.randomUUID(); var page = page(itemId);
        var decisions = new LinkedHashMap<UUID, kr.or.arte.seal.consent.application.ConsentDecision>(); decisions.put(itemId, kr.or.arte.seal.consent.application.ConsentDecision.AGREE);
        Instant submittedAt = Instant.parse("2026-09-06T03:04:05Z"); UUID idempotencyKey = UUID.randomUUID();
        given(query.load(FLOW)).willReturn(page);
        given(submissions.submit(FLOW, decisions, idempotencyKey)).willReturn(new kr.or.arte.seal.consent.application.ConsentSubmissionResult(UUID.randomUUID(), kr.or.arte.seal.consent.application.ConsentResponseStatus.SUBMITTED, false, submittedAt));
        var submitted = mvc.perform(post("/consent/respond/{flow}/submit", FLOW).with(csrf()).sessionAttr("SEAL_PUBLIC_FLOW", FLOW).sessionAttr("SEAL_PUBLIC_DECISIONS", decisions).sessionAttr("SEAL_PUBLIC_IDEMPOTENCY", idempotencyKey)).andExpect(status().is3xxRedirection()).andReturn();
        var receipt = (kr.or.arte.seal.consent.application.PublicConsentReceipt) submitted.getRequest().getSession().getAttribute("SEAL_PUBLIC_RECEIPT");
        assertThat(receipt.title()).isEqualTo("동의서 제목");
        assertThat(receipt.submittedAt()).isEqualTo(submittedAt);
        assertThat(receipt.items()).extracting(kr.or.arte.seal.consent.application.PublicConsentReceipt.Item::decisionLabel).containsExactly("동의");
        assertThat(receipt.withdrawalNotice()).contains("철회");
        mvc.perform(get("/consent/respond/{flow}/complete", FLOW).session((org.springframework.mock.web.MockHttpSession) submitted.getRequest().getSession())).andExpect(status().isOk()).andExpect(model().attributeExists("receipt"));
        mvc.perform(get("/consent/respond/{flow}/complete", FLOW).sessionAttr("SEAL_PUBLIC_FLOW", FLOW)).andExpect(status().isBadRequest());
    }

    @Test
    void startingAnotherFlowClearsPriorFlowStateAndCannotDisplayItsReceipt() throws Exception {
        String nextToken = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        String nextFlow = "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD";
        UUID itemId = UUID.randomUUID();
        var previousDecisions = Map.of(itemId, kr.or.arte.seal.consent.application.ConsentDecision.AGREE);
        var previousReceipt = new kr.or.arte.seal.consent.application.PublicConsentReceipt("이전 동의서",
                Instant.parse("2026-09-06T01:00:00Z"), List.of(), "철회 안내");
        given(entry.start(nextToken)).willReturn(new PublicConsentFlow(nextFlow, UUID.randomUUID(),
                Instant.parse("2026-09-07T01:00:00Z")));

        var started = mvc.perform(post("/consent/{token}/start", nextToken).with(csrf())
                        .sessionAttr("SEAL_PUBLIC_FLOW", FLOW)
                        .sessionAttr("SEAL_PUBLIC_DECISIONS", previousDecisions)
                        .sessionAttr("SEAL_PUBLIC_IDEMPOTENCY", UUID.randomUUID())
                        .sessionAttr("SEAL_PUBLIC_RECEIPT", previousReceipt)
                        .sessionAttr("SEAL_PUBLIC_RECEIPT_FLOW", FLOW))
                .andExpect(status().is3xxRedirection())
                .andExpect(request().sessionAttribute("SEAL_PUBLIC_FLOW", nextFlow))
                .andExpect(request().sessionAttributeDoesNotExist("SEAL_PUBLIC_DECISIONS",
                        "SEAL_PUBLIC_IDEMPOTENCY", "SEAL_PUBLIC_RECEIPT", "SEAL_PUBLIC_RECEIPT_FLOW"))
                .andReturn();

        mvc.perform(get("/consent/respond/{flow}/complete", nextFlow)
                        .session((org.springframework.mock.web.MockHttpSession) started.getRequest().getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completionRejectsAReceiptBoundToAnotherFlow() throws Exception {
        var receipt = new kr.or.arte.seal.consent.application.PublicConsentReceipt("이전 동의서",
                Instant.parse("2026-09-06T01:00:00Z"), List.of(), "철회 안내");
        mvc.perform(get("/consent/respond/{flow}/complete", FLOW)
                        .sessionAttr("SEAL_PUBLIC_FLOW", FLOW)
                        .sessionAttr("SEAL_PUBLIC_RECEIPT", receipt)
                        .sessionAttr("SEAL_PUBLIC_RECEIPT_FLOW", "another-flow"))
                .andExpect(status().isBadRequest());
    }

    private static kr.or.arte.seal.consent.application.PublicConsentPage page(UUID itemId) {
        return new kr.or.arte.seal.consent.application.PublicConsentPage("기관", "동의서 제목", "안내", "처리업무", "처리목적", "1년", "담당 부서에 철회를 요청할 수 있습니다.", Instant.parse("2026-09-10T00:00:00Z"), List.of(new kr.or.arte.seal.consent.application.PublicConsentItem(itemId, 1, "필수 항목", true, "목적", "1년", "거부 안내", "이름, 이메일", "제공 기관")));
    }
}
