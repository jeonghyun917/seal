package kr.or.arte.seal.consent.scheduling;

import kr.or.arte.seal.consent.application.PublicConsentSessionExpiryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "seal.public-consent.session-expiry", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PublicConsentSessionExpiryJob {
    private final PublicConsentSessionExpiryService service;

    public PublicConsentSessionExpiryJob(PublicConsentSessionExpiryService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${seal.public-consent.session-expiry.fixed-delay:60000}")
    public void expireDueSessions() {
        service.expireDue(100);
    }
}
