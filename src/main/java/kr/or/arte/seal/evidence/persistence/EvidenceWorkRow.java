package kr.or.arte.seal.evidence.persistence;

import java.time.Instant;
import java.util.UUID;

public record EvidenceWorkRow(UUID packageId, UUID responseId, UUID requestId, UUID organizationId,
        UUID recipientId, String requestCode, String templateCode, String templateName,
        int templateVersionNumber, String affiliationName, String jobTitle,
        byte[] nameCiphertext, byte[] emailCiphertext, String encryptionKeyId,
        Instant requestCreatedAt, Instant firstSentAt, Instant expiresAt, String snapshotJson,
        String snapshotHash, String responseStatus, Instant submittedAt, int attemptCount,
        long rowVersion) {

    public EvidenceWorkRow {
        nameCiphertext = copy(nameCiphertext);
        emailCiphertext = copy(emailCiphertext);
    }

    @Override public byte[] nameCiphertext() { return copy(nameCiphertext); }
    @Override public byte[] emailCiphertext() { return copy(emailCiphertext); }

    private static byte[] copy(byte[] value) { return value == null ? null : value.clone(); }
}
