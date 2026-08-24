package kr.or.arte.seal.request.scheduling;

import kr.or.arte.seal.request.application.ConsentRequestExpiryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConsentRequestExpiryJob {
    private final ConsentRequestExpiryService service;
    public ConsentRequestExpiryJob(ConsentRequestExpiryService service) { this.service=service; }
    @Scheduled(fixedDelayString="${seal.request-expiry.fixed-delay:60000}")
    public void expireDueRequests() { service.expireDue(100); }
}
