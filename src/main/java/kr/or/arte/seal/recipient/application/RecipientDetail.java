package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.domain.RecipientStatus;

import java.util.UUID;

public record RecipientDetail(UUID recipientId, UUID organizationUnitId,
                              String affiliationName, String jobTitle,
                              String maskedName, String maskedEmail,
                              String collectionBasis, RecipientStatus status,
                              long versionNo) {
    public RecipientDetail(UUID recipientId, UUID organizationUnitId, String affiliationName,
                           String maskedName, String maskedEmail, String collectionBasis,
                           RecipientStatus status, long versionNo) {
        this(recipientId, organizationUnitId, affiliationName, null, maskedName, maskedEmail,
                collectionBasis, status, versionNo);
    }

    public RecipientDetail(UUID recipientId, UUID organizationUnitId,
                           String maskedName, String maskedEmail, String collectionBasis,
                           RecipientStatus status, long versionNo) {
        this(recipientId, organizationUnitId, null, null, maskedName, maskedEmail,
                collectionBasis, status, versionNo);
    }
}