package kr.or.arte.seal.recipient.application;
import java.util.UUID;
public record RecipientDeliveryContact(UUID recipientId,UUID organizationId,String name,String email) {}
