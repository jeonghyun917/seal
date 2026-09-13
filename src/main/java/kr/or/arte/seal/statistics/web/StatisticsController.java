package kr.or.arte.seal.statistics.web;

import jakarta.servlet.http.HttpServletResponse;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.statistics.application.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.time.*;

@Controller
public class StatisticsController {
    public static final String EXPORT_ACCESS = "hasAuthority('STATISTICS_VIEW') and hasAuthority('CONSENT_REQUEST_VIEW') and hasAuthority('RECIPIENT_VIEW') and hasAuthority('MAIL_VIEW') and hasAuthority('EVIDENCE_VIEW') and hasAuthority('RIGHTS_VIEW') and hasAuthority('RETENTION_VIEW')";
    private final StatisticsQueryService service;
    private final Clock clock;
    private final StatisticsExportService exports;
    public StatisticsController(StatisticsQueryService service,Clock clock,StatisticsExportService exports){
        this.service=service;this.clock=clock;this.exports=exports;
    }
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasAuthority('STATISTICS_VIEW')")
    public String index(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AdminPrincipal p,Model m){
        if(to==null)to=LocalDate.now(clock);
        if(from==null)from=to.minusDays(29);
        m.addAttribute("administrator",p);m.addAttribute("from",from);m.addAttribute("to",to);
        m.addAttribute("statistics",service.query(p.adminUserId(),from,to));
        return "admin/statistics/index";
    }
    @GetMapping("/admin/statistics/export")
    @PreAuthorize(EXPORT_ACCESS)
    public void export(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AdminPrincipal p,HttpServletResponse response) throws IOException {
        if(to==null)to=LocalDate.now(clock);
        if(from==null)from=to.minusDays(29);
        if(to.isBefore(from)||to.isAfter(from.plusYears(1)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"통계 기간은 순방향 최대 1년입니다.");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition","attachment; filename=\"statistics-"+from+"_"+to+".xlsx\"");
        response.setHeader("Cache-Control","no-store");
        exports.write(p.adminUserId(),from,to,response.getOutputStream());
    }
}
