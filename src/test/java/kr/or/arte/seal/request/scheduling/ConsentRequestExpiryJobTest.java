package kr.or.arte.seal.request.scheduling;

import kr.or.arte.seal.request.application.ConsentRequestExpiryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConsentRequestExpiryJobTest {
    @Test
    void delegatesOneBoundedExpiryBatch() {
        ConsentRequestExpiryService service = mock(ConsentRequestExpiryService.class);
        new ConsentRequestExpiryJob(service).expireDueRequests();
        verify(service).expireDue(100);
    }
}
