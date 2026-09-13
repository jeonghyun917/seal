package kr.or.arte.seal.admin.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.statistics.application.StatisticsQueryService;
import kr.or.arte.seal.statistics.application.StatisticsSnapshot;
import kr.or.arte.seal.statistics.application.StatusCount;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminHomeController {
    private final StatisticsQueryService statistics;
    private final Clock clock;

    public AdminHomeController(StatisticsQueryService statistics, Clock clock) {
        this.statistics = statistics;
        this.clock = clock;
    }

    @GetMapping("/admin")
    public String home(@AuthenticationPrincipal AdminPrincipal principal, Authentication authentication, Model model) {
        model.addAttribute("administrator", principal);
        List<AdminHomeWorkItem> workItems = new ArrayList<>();
        if (hasAnyOperationalView(authentication)) {
            LocalDate to = LocalDate.now(clock);
            StatisticsSnapshot snapshot = statistics.queryCurrentWork(principal.adminUserId(), to);
            addWorkItems(workItems, snapshot, authentication);
            model.addAttribute("workItemTo", to);
        }
        model.addAttribute("workItems", List.copyOf(workItems));
        return "admin/home";
    }

    private static boolean hasAnyOperationalView(Authentication authentication) {
        return has(authentication, "MAIL_VIEW") || has(authentication, "EVIDENCE_VIEW")
                || has(authentication, "RIGHTS_VIEW") || has(authentication, "RETENTION_VIEW");
    }

    private static void addWorkItems(List<AdminHomeWorkItem> items, StatisticsSnapshot snapshot,
                                     Authentication authentication) {
        if (has(authentication, "MAIL_VIEW")) {
            add(items, "발송 실패 메일", count(snapshot.mail(), "SEND_FAILED"), "/admin/mail-outbox?status=SEND_FAILED");
        }
        if (has(authentication, "EVIDENCE_VIEW")) {
            add(items, "생성 실패 증적", count(snapshot.evidence(), "FAILED"), "/admin/evidence?status=FAILED");
        }
        if (has(authentication, "RIGHTS_VIEW")) {
            add(items, "처리기한이 지난 권리요청", count(snapshot.rightsWarnings(), "OVERDUE"), "/admin/rights");
        }
        if (has(authentication, "RETENTION_VIEW")) {
            add(items, "처리 대기 파기 작업", count(snapshot.retention(), "DUE"), "/admin/retention?status=DUE");
            add(items, "실패한 파기 작업", count(snapshot.retention(), "FAILED"), "/admin/retention?status=FAILED");
        }
    }

    private static void add(List<AdminHomeWorkItem> items, String label, long count, String href) {
        if (count > 0) items.add(new AdminHomeWorkItem(label, count, href));
    }

    private static long count(List<StatusCount> counts, String status) {
        return counts.stream().filter(row -> status.equals(row.status())).mapToLong(StatusCount::count).sum();
    }

    private static boolean has(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
