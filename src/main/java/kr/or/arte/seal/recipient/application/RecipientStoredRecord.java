package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.domain.RecipientStatus;

import java.util.UUID;

public record RecipientStoredRecord(UUID recipientId, UUID organizationUnitId,
                                    byte[] nameCiphertext, byte[] emailCiphertext,
                                    String encryptionKeyId, String emailLookupHmac,
                                    String collectionBasis, RecipientStatus status,
                                    long versionNo) {
    public RecipientStoredRecord {
        nameCiphertext = nameCiphertext.clone();
        emailCiphertext = emailCiphertext.clone();
    }

    @Override public byte[] nameCiphertext() { return nameCiphertext.clone(); }
    @Override public byte[] emailCiphertext() { return emailCiphertext.clone(); }
}
