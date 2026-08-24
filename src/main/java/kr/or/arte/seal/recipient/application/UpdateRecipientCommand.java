package kr.or.arte.seal.recipient.application;

import java.util.UUID;

public record UpdateRecipientCommand(UUID actorId, String name, String email,
                                     String collectionBasis, long versionNo) {
}
