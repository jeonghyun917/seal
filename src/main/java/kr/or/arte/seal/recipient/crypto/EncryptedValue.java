package kr.or.arte.seal.recipient.crypto;

public record EncryptedValue(String keyId, byte[] bytes) {
    public EncryptedValue {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("암호화 키 식별자가 필요합니다.");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("암호문이 필요합니다.");
        }
        keyId = keyId.trim();
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
