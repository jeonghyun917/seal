package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.domain.RecipientStatus;

import java.util.UUID;

public record RecipientStoredRecord(UUID recipientId, UUID organizationUnitId,
                                    String affiliationName, String jobTitle,
                                    byte[] nameCiphertext, byte[] emailCiphertext,
                                    String encryptionKeyId, String emailLookupHmac,
                                    String collectionBasis, RecipientStatus status,
                                    long versionNo) {
    public RecipientStoredRecord {
        nameCiphertext = copy(nameCiphertext);
        emailCiphertext = copy(emailCiphertext);
    }

    public RecipientStoredRecord(UUID recipientId, UUID organizationUnitId, String affiliationName,
                                 byte[] nameCiphertext, byte[] emailCiphertext,
                                 String encryptionKeyId, String emailLookupHmac,
                                 String collectionBasis, RecipientStatus status, long versionNo) {
        this(recipientId, organizationUnitId, affiliationName, null, nameCiphertext, emailCiphertext,
                encryptionKeyId, emailLookupHmac, collectionBasis, status, versionNo);
    }

    public RecipientStoredRecord(UUID recipientId, UUID organizationUnitId,
                                 byte[] nameCiphertext, byte[] emailCiphertext,
                                 String encryptionKeyId, String emailLookupHmac,
                                 String collectionBasis, RecipientStatus status,
                                 long versionNo) {
        this(recipientId, organizationUnitId, null, null, nameCiphertext, emailCiphertext,
                encryptionKeyId, emailLookupHmac, collectionBasis, status, versionNo);
    }

    @Override public byte[] nameCiphertext() { return copy(nameCiphertext); }
    @Override public byte[] emailCiphertext() { return copy(emailCiphertext); }

    private static byte[] copy(byte[] value) { return value == null ? null : value.clone(); }
}