package kr.or.arte.seal.recipient.application;

import java.util.UUID;

public record CreateRecipientCommand(UUID actorId, UUID organizationUnitId,
                                     String name, String email, String collectionBasis) {
}
