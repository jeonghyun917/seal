package kr.or.arte.seal.delivery.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MailOutboxScheduler {
    private final MailOutboxWorker worker;

    public MailOutboxScheduler(MailOutboxWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${seal.mail.poll-delay:60s}")
    public void sendDueMail() {
        worker.processBatch(20);
    }
}
