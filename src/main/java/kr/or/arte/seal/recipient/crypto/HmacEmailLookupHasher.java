package kr.or.arte.seal.recipient.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class HmacEmailLookupHasher implements EmailLookupHasher {
    private static final String ALGORITHM = "HmacSHA256";
    private final byte[] key;

    public HmacEmailLookupHasher(byte[] key) {
        if (key == null || key.length < 32) {
            throw new IllegalArgumentException("이메일 검색 HMAC 키는 32바이트 이상이어야 합니다.");
        }
        this.key = key.clone();
    }

    @Override
    public String hmac(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("정규화된 이메일이 필요합니다.");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(normalizedEmail.getBytes(UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new PersonalDataProtectionException("이메일 검색값 생성에 실패했습니다.", exception);
        }
    }
}
