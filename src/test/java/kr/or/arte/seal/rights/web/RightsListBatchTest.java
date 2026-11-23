package kr.or.arte.seal.rights.web;
import kr.or.arte.seal.rights.application.*;
import kr.or.arte.seal.recipient.application.*;
import kr.or.arte.seal.request.application.*;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.common.web.AdminPageResult;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class RightsListBatchTest {
 @Test void listLoadsMaskedRecipientContextOnce() {
  var service=mock(RightsRequestService.class);var recipients=mock(RecipientQueryService.class);
  UUID actor=UUID.randomUUID();
  when(service.searchPage(actor,null,1,20)).thenReturn(new AdminPageResult<>(List.of(),1,20,false));
  when(recipients.findMany(eq(actor),any())).thenReturn(Map.of());
  var model=new ExtendedModelMap();
  new RightsRequestController(service,recipients,mock(ConsentRequestQueryService.class)).list(null,1,new AdminPrincipal(actor,"admin","관리자",UUID.randomUUID(),false),model);
  assertThat(model).containsKey("recipientDetails");
  verify(recipients).findMany(eq(actor),any());verify(recipients,never()).find(any(),any());
 }
}
