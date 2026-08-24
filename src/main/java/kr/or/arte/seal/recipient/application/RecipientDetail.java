package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.domain.RecipientStatus;

import java.util.UUID;

public record RecipientDetail(UUID recipientId, UUID organizationUnitId,
                              String maskedName, String maskedEmail,
                              String collectionBasis, RecipientStatus status,
                              long versionNo) {
}
