package kr.or.arte.seal.request.web;
import kr.or.arte.seal.request.application.*;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import kr.or.arte.seal.recipient.application.*;
import kr.or.arte.seal.template.application.*;
import kr.or.arte.seal.delivery.application.ConsentDeliveryService;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.common.web.AdminPageResult;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import java.util.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestListBatchTest {
 @Test void listUsesThreePageQueriesInsteadOfPerRowDetailQueries(){
  var query=mock(ConsentRequestQueryService.class);var recipients=mock(RecipientQueryService.class);
  var templates=mock(PublishedConsentTemplateQuery.class);UUID actor=UUID.randomUUID(),recipient=UUID.randomUUID(),version=UUID.randomUUID();
  var rows=java.util.stream.IntStream.range(0,20).mapToObj(i->new ConsentRequestSummary(UUID.randomUUID(),"CR-"+i,recipient,ConsentRequestStatus.READY,Instant.now(),Instant.now(),0,version)).toList();
  when(query.searchPage(actor,null,null,1,20)).thenReturn(new AdminPageResult<>(rows,1,20,false));
  when(recipients.findMany(eq(actor),any())).thenReturn(Map.of());
  when(templates.findVersionOptions(eq(actor),anyList())).thenReturn(Map.of(version,new PublishedConsentTemplateOption(version,"FORM","동의서","업무",1)));
  var controller=new ConsentRequestController(mock(ConsentRequestManagement.class),query,mock(ConsentDeliveryService.class),recipients,templates,new com.fasterxml.jackson.databind.ObjectMapper(),"http://localhost");
  var model=new ExtendedModelMap();
  assertThat(controller.list(null,null,1,new AdminPrincipal(actor,"admin","관리자",UUID.randomUUID(),false),model)).isEqualTo("admin/requests/list");
  assertThat((List<?>)model.get("requests")).hasSize(20);
  verify(query,never()).require(any(),any());verify(recipients,never()).find(any(),any());verify(templates,never()).findVersionOption(any(),any());
  verify(recipients,times(1)).findMany(eq(actor),any());verify(templates,times(1)).findVersionOptions(eq(actor),anyList());
 }
}
