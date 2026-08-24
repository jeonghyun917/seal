package kr.or.arte.seal.recipient.application;

import java.util.UUID;

public final class DuplicateRecipientException extends RuntimeException {
    private final UUID existingRecipientId;

    public DuplicateRecipientException(UUID existingRecipientId, String maskedEmail) {
        super("같은 조직에 활성 상태인 대상자가 이미 있습니다: " + maskedEmail);
        this.existingRecipientId = existingRecipientId;
    }

    public UUID existingRecipientId() { return existingRecipientId; }
}
