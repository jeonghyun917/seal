package kr.or.arte.seal.evidence.infrastructure;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

public final class EvidenceHmacSigner {
    private final byte[] key;

    public EvidenceHmacSigner(Path keyFile) {
        try {
            key = Files.readAllBytes(keyFile);
            if (key.length != 32) throw new IllegalArgumentException("증적 HMAC 키는 32바이트여야 합니다.");
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("증적 HMAC 키를 읽을 수 없습니다.", exception);
        }
    }

    public String sign(byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public boolean verify(byte[] value, String signature) {
        if (signature == null) return false;
        return MessageDigest.isEqual(sign(value).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                signature.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
}
