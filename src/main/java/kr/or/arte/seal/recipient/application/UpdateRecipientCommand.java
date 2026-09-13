package kr.or.arte.seal.recipient.application;

import java.util.UUID;

public record UpdateRecipientCommand(UUID actorId, String affiliationName, String jobTitle,
                                     String name, String email,
                                     String collectionBasis, long versionNo) {
    public UpdateRecipientCommand(UUID actorId, String affiliationName, String name, String email,
                                  String collectionBasis, long versionNo) {
        this(actorId, affiliationName, null, name, email, collectionBasis, versionNo);
    }

    public UpdateRecipientCommand(UUID actorId, String name, String email,
                                  String collectionBasis, long versionNo) {
        this(actorId, null, null, name, email, collectionBasis, versionNo);
    }
}