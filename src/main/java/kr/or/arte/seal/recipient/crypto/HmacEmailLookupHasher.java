package kr.or.arte.seal.recipient.crypto;

import kr.or.arte.seal.recipient.config.LoadedHmacKeys;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class HmacEmailLookupHasher implements EmailLookupHasher {
    private static final String ALGORITHM = "HmacSHA256";
    private final String activeKeyId;
    private final Map<String, byte[]> keys;

    public HmacEmailLookupHasher(byte[] key) {
        this(new LoadedHmacKeys("v1", Map.of("v1", key)));
    }

    public HmacEmailLookupHasher(LoadedHmacKeys loaded) {
        this.activeKeyId = loaded.activeKeyId();
        Map<String, byte[]> ordered = new LinkedHashMap<>();
        ordered.put(activeKeyId, valid(loaded.keys().get(activeKeyId)));
        loaded.keys().forEach((id, key) -> ordered.putIfAbsent(id, valid(key)));
        this.keys = Map.copyOf(ordered);
    }

    @Override
    public String hmac(String normalizedEmail) {
        return hmac(normalizedEmail, keys.get(activeKeyId));
    }

    @Override
    public List<String> candidates(String normalizedEmail) {
        List<String> values = new ArrayList<>();
        values.add(hmac(normalizedEmail));
        keys.forEach((id, key) -> {
            if (!id.equals(activeKeyId)) values.add(hmac(normalizedEmail, key));
        });
        return List.copyOf(values);
    }

    private static String hmac(String normalizedEmail, byte[] key) {
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

    private static byte[] valid(byte[] key) {
        if (key == null || key.length < 32) {
            throw new IllegalArgumentException("이메일 검색 HMAC 키는 32바이트 이상이어야 합니다.");
        }
        return key.clone();
    }
}
