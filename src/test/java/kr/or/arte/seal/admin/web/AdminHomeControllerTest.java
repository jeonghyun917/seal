package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.statistics.application.StatisticsQueryService;
import kr.or.arte.seal.statistics.application.StatisticsSnapshot;
import kr.or.arte.seal.statistics.application.StatusCount;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ConcurrentModel;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdminHomeControllerTest {

    private static final AdminPrincipal PRINCIPAL = new AdminPrincipal(UUID.randomUUID(), "admin", "관리자",
            UUID.randomUUID(), false);

    @Test
    void exposesOnlyWorkItemsWhoseListViewIsAuthorized() {
        StatisticsQueryService statistics = mock(StatisticsQueryService.class);
        given(statistics.queryCurrentWork(any(), any())).willReturn(snapshot());
        var controller = new AdminHomeController(statistics,
                Clock.fixed(Instant.parse("2026-09-06T00:00:00Z"), ZoneOffset.UTC));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(PRINCIPAL, null,
                List.of(new SimpleGrantedAuthority("RIGHTS_VIEW")));
        var model = new ConcurrentModel();

        assertThat(controller.home(PRINCIPAL, authentication, model)).isEqualTo("admin/home");

        assertThat((List<AdminHomeWorkItem>) model.getAttribute("workItems"))
                .extracting(AdminHomeWorkItem::label, AdminHomeWorkItem::count, AdminHomeWorkItem::href)
                .containsExactly(tuple("처리기한이 지난 권리요청", 1L, "/admin/rights"));
    }

    @Test
    void doesNotQueryScopedCountsWhenNoOperationalListIsAuthorized() {
        StatisticsQueryService statistics = mock(StatisticsQueryService.class);
        var controller = new AdminHomeController(statistics,
                Clock.fixed(Instant.parse("2026-09-06T00:00:00Z"), ZoneOffset.UTC));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(PRINCIPAL, null,
                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));

        controller.home(PRINCIPAL, authentication, new ConcurrentModel());

        verify(statistics, never()).queryCurrentWork(any(), any());
    }

    private static StatisticsSnapshot snapshot() {
        return new StatisticsSnapshot(List.of(), List.of(), List.of(new StatusCount("WITHDRAWN", 2)),
                List.of(), List.of(), List.of(new StatusCount("VERIFY_INVALID_HASH", 1)),
                List.of(new StatusCount("IN_REVIEW", 1)), List.of(new StatusCount("OVERDUE", 1)),
                List.of(), List.of());
    }
}
