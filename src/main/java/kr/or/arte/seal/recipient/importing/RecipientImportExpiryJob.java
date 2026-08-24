package kr.or.arte.seal.recipient.importing;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
@Component public class RecipientImportExpiryJob {private final RecipientImportService service;public RecipientImportExpiryJob(RecipientImportService service){this.service=service;}@Scheduled(fixedDelayString="${seal.recipient-import.expiry-job-delay:PT10M}") public void expire(){service.expireDue(100);}}
