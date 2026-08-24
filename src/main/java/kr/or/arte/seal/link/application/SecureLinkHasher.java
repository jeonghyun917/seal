package kr.or.arte.seal.link.application;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class SecureLinkHasher {
    private final byte[] key;
    public SecureLinkHasher(byte[] key) {
        if (key == null || key.length < 32) throw new IllegalArgumentException("링크 HMAC 키는 32바이트 이상이어야 합니다.");
        this.key = key.clone();
    }
    public String hmac(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(token.getBytes(US_ASCII)));
        } catch (GeneralSecurityException exception) { throw new IllegalStateException("링크 해시에 실패했습니다.", exception); }
    }
    public boolean matches(String token, String expectedHex) {
        return MessageDigest.isEqual(HexFormat.of().parseHex(hmac(token)), HexFormat.of().parseHex(expectedHex));
    }
}
