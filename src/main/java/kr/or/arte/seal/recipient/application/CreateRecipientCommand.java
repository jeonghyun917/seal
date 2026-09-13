package kr.or.arte.seal.recipient.application;

import java.util.UUID;

public record CreateRecipientCommand(UUID actorId, UUID organizationUnitId,
                                     String affiliationName, String jobTitle, String name,
                                     String email, String collectionBasis) {
    public CreateRecipientCommand(UUID actorId, UUID organizationUnitId, String affiliationName,
                                  String name, String email, String collectionBasis) {
        this(actorId, organizationUnitId, affiliationName, null, name, email, collectionBasis);
    }

    public CreateRecipientCommand(UUID actorId, UUID organizationUnitId, String name,
                                  String email, String collectionBasis) {
        this(actorId, organizationUnitId, null, null, name, email, collectionBasis);
    }
}