package kr.or.arte.seal.delivery.application;
import kr.or.arte.seal.link.application.IssuedSecureLink;import java.util.UUID;
public record DeliveryIssueResult(UUID outboxId,IssuedSecureLink issuedLink,MailOutboxStatus mailStatus) {}
