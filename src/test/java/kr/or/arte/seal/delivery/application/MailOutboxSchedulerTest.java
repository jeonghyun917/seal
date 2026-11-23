package kr.or.arte.seal.delivery.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailOutboxSchedulerTest {
    @Test void passesConfiguredBatchSizeWithoutSendingAnyRealMail() {
        var worker=mock(MailOutboxWorker.class);
        new MailOutboxScheduler(worker,20).sendDueMail();
        verify(worker).processBatch(20);
    }
    @Test void rejectsUnsafeBatchConfiguration() {
        var worker=mock(MailOutboxWorker.class);
        assertThatThrownBy(()->new MailOutboxScheduler(worker,0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new MailOutboxScheduler(worker,101)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(worker);
    }
}
