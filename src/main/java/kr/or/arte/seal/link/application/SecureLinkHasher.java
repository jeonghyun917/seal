package kr.or.arte.seal.link.application;

import kr.or.arte.seal.recipient.config.LoadedHmacKeys;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class SecureLinkHasher {
    private final String activeKeyId;
    private final Map<String, byte[]> keys;

    public SecureLinkHasher(byte[] key) {
        this(new LoadedHmacKeys("v1", Map.of("v1", key)));
    }

    public SecureLinkHasher(LoadedHmacKeys loaded) {
        this.activeKeyId = loaded.activeKeyId();
        Map<String, byte[]> ordered = new LinkedHashMap<>();
        ordered.put(activeKeyId, valid(loaded.keys().get(activeKeyId)));
        loaded.keys().forEach((id, key) -> ordered.putIfAbsent(id, valid(key)));
        this.keys = Map.copyOf(ordered);
    }

    public String hmac(String token) {
        return hmac(token, keys.get(activeKeyId));
    }

    public List<String> candidates(String token) {
        List<String> values = new ArrayList<>();
        values.add(hmac(token));
        keys.forEach((id, key) -> {
            if (!id.equals(activeKeyId)) values.add(hmac(token, key));
        });
        return List.copyOf(values);
    }

    public boolean matches(String token, String expectedHex) {
        return candidates(token).stream().anyMatch(candidate -> MessageDigest.isEqual(
                HexFormat.of().parseHex(candidate), HexFormat.of().parseHex(expectedHex)));
    }

    private static String hmac(String token, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(token.getBytes(US_ASCII)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("링크 해시에 실패했습니다.", exception);
        }
    }

    private static byte[] valid(byte[] key) {
        if (key == null || key.length < 32) {
            throw new IllegalArgumentException("링크 HMAC 키는 32바이트 이상이어야 합니다.");
        }
        return key.clone();
    }
}
