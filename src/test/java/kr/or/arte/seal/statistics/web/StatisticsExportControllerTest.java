package kr.or.arte.seal.statistics.web;
import kr.or.arte.seal.statistics.application.*;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsExportControllerTest {
    @Configuration @EnableMethodSecurity static class Security {}
    @Test void requiresEveryDetailPermissionAndValidatesDateRange() throws Exception {
        try(var context=new AnnotationConfigApplicationContext()) {
            var export=mock(StatisticsExportService.class);
            context.register(Security.class);
            context.registerBean(StatisticsController.class,()->new StatisticsController(mock(StatisticsQueryService.class),Clock.systemUTC(),export));
            context.refresh();
            var controller=context.getBean(StatisticsController.class);
            var principal=new AdminPrincipal(UUID.randomUUID(),"test","test",UUID.randomUUID(),false);
            var from=LocalDate.of(2026,8,10);var to=LocalDate.of(2026,9,8);
            String[] permissions={"STATISTICS_VIEW","CONSENT_REQUEST_VIEW","RECIPIENT_VIEW","MAIL_VIEW","EVIDENCE_VIEW","RIGHTS_VIEW","RETENTION_VIEW"};
            for(String missing:permissions) {
                var granted=Arrays.stream(permissions).filter(p->!p.equals(missing)).map(SimpleGrantedAuthority::new).toList();
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",granted));
                assertThatThrownBy(()->controller.export(from,to,principal,new MockHttpServletResponse())).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoInteractions(export);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
            assertThatThrownBy(()->controller.export(to,from,principal,new MockHttpServletResponse())).isInstanceOf(ResponseStatusException.class);
            assertThatThrownBy(()->controller.export(from,from.plusYears(1).plusDays(1),principal,new MockHttpServletResponse())).isInstanceOf(ResponseStatusException.class);
            verifyNoInteractions(export);
            var response=new MockHttpServletResponse();
            controller.export(from,to,principal,response);
            verify(export).write(eq(principal.adminUserId()),eq(from),eq(to),any());
            assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
            assertThat(response.getHeader("Content-Disposition")).contains(".xlsx");
        } finally {SecurityContextHolder.clearContext();}
    }
}
