package kr.or.arte.seal.delivery.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MailOutboxScheduler {
    private final MailOutboxWorker worker;
    private final int batchSize;

    public MailOutboxScheduler(MailOutboxWorker worker,
            @org.springframework.beans.factory.annotation.Value("${seal.mail.batch-size:20}") int batchSize) {
        this.worker = worker;
        if (batchSize < 1 || batchSize > 100) throw new IllegalArgumentException("메일 배치 크기는 1~100이어야 합니다.");
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${seal.mail.poll-delay:60s}")
    public void sendDueMail() {
        worker.processBatch(batchSize);
    }
}
