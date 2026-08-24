package kr.or.arte.seal.recipient.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class AesGcmPersonalDataCipher implements PersonalDataCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final byte FORMAT_VERSION = 1;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int MINIMUM_STORED_BYTES = 1 + NONCE_BYTES + 16;

    private final Map<String, SecretKey> keys;
    private final String activeKeyId;
    private final SecureRandom secureRandom;

    public AesGcmPersonalDataCipher(Map<String, SecretKey> keys, String activeKeyId,
                                    SecureRandom secureRandom) {
        this.keys = Map.copyOf(keys);
        this.activeKeyId = Objects.requireNonNull(activeKeyId, "activeKeyId");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
        if (!this.keys.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("활성 암호화 키를 찾을 수 없습니다.");
        }
    }

    @Override
    public EncryptedValue encrypt(PersonalDataContext context, String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("암호화할 값이 필요합니다.");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeKeyId),
                    new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(context));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(UTF_8));
            ByteBuffer stored = ByteBuffer.allocate(1 + nonce.length + encrypted.length);
            stored.put(FORMAT_VERSION).put(nonce).put(encrypted);
            return new EncryptedValue(activeKeyId, stored.array());
        } catch (GeneralSecurityException exception) {
            throw new PersonalDataProtectionException("개인정보 암호화에 실패했습니다.", exception);
        }
    }

    @Override
    public String decrypt(PersonalDataContext context, EncryptedValue value) {
        SecretKey key = keys.get(value.keyId());
        if (key == null) {
            throw new PersonalDataProtectionException("암호화 키 버전을 찾을 수 없습니다.");
        }
        byte[] stored = value.bytes();
        if (stored.length < MINIMUM_STORED_BYTES || stored[0] != FORMAT_VERSION) {
            throw new PersonalDataProtectionException("지원하지 않는 개인정보 암호문입니다.");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        byte[] encrypted = new byte[stored.length - 1 - NONCE_BYTES];
        System.arraycopy(stored, 1, nonce, 0, nonce.length);
        System.arraycopy(stored, 1 + NONCE_BYTES, encrypted, 0, encrypted.length);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(context));
            return new String(cipher.doFinal(encrypted), UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new PersonalDataProtectionException("개인정보 복호화에 실패했습니다.", exception);
        }
    }

    private static byte[] aad(PersonalDataContext context) {
        return String.join("\0", context.aggregate(), context.field(),
                context.objectId().toString(), context.organizationId().toString()).getBytes(UTF_8);
    }
}
