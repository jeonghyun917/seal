package kr.or.arte.seal.evidence.application;

import org.springframework.scheduling.annotation.Scheduled;

public final class EvidenceWorkerScheduler {
    private final EvidenceWorker worker;
    public EvidenceWorkerScheduler(EvidenceWorker worker){this.worker=worker;}
    @Scheduled(fixedDelayString="${seal.evidence.poll-delay:60s}") public void process(){worker.processBatch(10);}
}
